package com.delosreyes.inventario.modules.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.delosreyes.inventario.config.JwtProperties;
import com.delosreyes.inventario.modules.audit.service.AuditService;
import com.delosreyes.inventario.modules.auth.dto.AuthDtos;
import com.delosreyes.inventario.modules.auth.entity.PasswordResetToken;
import com.delosreyes.inventario.modules.auth.entity.RefreshToken;
import com.delosreyes.inventario.modules.auth.repository.PasswordResetTokenRepository;
import com.delosreyes.inventario.modules.auth.repository.RefreshTokenRepository;
import com.delosreyes.inventario.modules.receipts.service.EmailService;
import com.delosreyes.inventario.modules.users.entity.User;
import com.delosreyes.inventario.modules.users.repository.UserRepository;
import com.delosreyes.inventario.modules.users.service.AvatarStorageService;
import com.delosreyes.inventario.security.service.JwtService;
import com.delosreyes.inventario.shared.exceptions.BusinessException;
import com.delosreyes.inventario.shared.exceptions.DuplicateResourceException;
import com.delosreyes.inventario.shared.exceptions.ResourceNotFoundException;
import com.delosreyes.inventario.shared.exceptions.UnauthorizedException;

/**
 * AuthService — Lógica de autenticación JWT.
 *
 * LOGIN:
 *   authenticationManager.authenticate() valida email, contraseña y estado.
 *   User implementa UserDetails: isEnabled() es false si está INACTIVE
 *   (DisabledException) e isAccountNonLocked() es false si está SUSPENDED
 *   (LockedException). Las excepciones de Spring Security se relanzan tal cual
 *   para que GlobalExceptionHandler las convierta en la respuesta HTTP correcta.
 *
 *   Los logs distinguen un error de credenciales de un error interno.
 */
@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AvatarStorageService avatarStorageService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final String frontendUrl;
    private final int resetExpirationMinutes;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService,
                       JwtProperties jwtProperties,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService,
                       AvatarStorageService avatarStorageService,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       EmailService emailService,
                       @Value("${app.frontend.url}") String frontendUrl,
                       @Value("${app.password-reset.expiration-minutes:30}") int resetExpirationMinutes) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.avatarStorageService = avatarStorageService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
        this.resetExpirationMinutes = resetExpirationMinutes;
    }

    // LOGIN

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request, String ipAddress) {

        log.debug("Intento de login para: {}", request.getEmail());

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );
        } catch (BadCredentialsException e) {
            log.warn("Credenciales inválidas para: {}", request.getEmail());
            throw e;
        } catch (DisabledException e) {
            log.warn("Cuenta desactivada: {}", request.getEmail());
            throw e;
        } catch (LockedException e) {
            log.warn("Cuenta bloqueada/suspendida: {}", request.getEmail());
            throw e;
        }

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> {
                log.error("Usuario autenticado correctamente pero no encontrado en BD: {}",
                    request.getEmail());
                return new ResourceNotFoundException("Usuario", "email", request.getEmail());
            });

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().getName());
        extraClaims.put("userId", user.getId());

        String accessToken = jwtService.generateAccessToken(user, extraClaims);
        String refreshToken = jwtService.generateRefreshToken(user);

        saveRefreshToken(user, refreshToken);

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        try {
            auditService.logLogin(user.getId(), user.getEmail(), ipAddress);
        } catch (Exception e) {
            log.error("Error al registrar auditoría de login para {}: {}", 
                user.getEmail(), e.getMessage());
        }

        log.info("Login exitoso: {} [{}] desde IP: {}", 
            user.getEmail(), user.getRole().getName(), ipAddress);

        return AuthDtos.LoginResponse.builder()
            .userId(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .role(user.getRole().getName())
            .avatarUrl(user.getAvatarUrl())
            .permissions(user.getPermissionCodes())
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(jwtProperties.getExpiration())
            .build();
    }
        // REFRESH TOKEN

    @Transactional(readOnly = false)
    // El token llega desde la cookie HttpOnly (ver AuthController.refresh)
    public AuthDtos.LoginResponse refreshToken(String tokenValue) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenValue)
            .orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));

        if (!refreshToken.isValid()) {
            throw new UnauthorizedException("Refresh token expirado o revocado. Inicie sesión nuevamente.");
        }

        User user = refreshToken.getUser();

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().getName());
        extraClaims.put("userId", user.getId());

        String newAccessToken = jwtService.generateAccessToken(user, extraClaims);

        log.debug("Access token renovado para: {}", user.getEmail());

        return AuthDtos.LoginResponse.builder()
            .userId(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .role(user.getRole().getName())
            .avatarUrl(user.getAvatarUrl())
            .permissions(user.getPermissionCodes())
            .accessToken(newAccessToken)
            .refreshToken(tokenValue)
            .expiresIn(jwtProperties.getExpiration())
            .build();
    }


    // LOGOUT

    public void logout(User user) {
        refreshTokenRepository.revokeAllByUserId(user.getId());
        auditService.logLogout(user.getId(), user.getEmail());
        log.info("Logout: {}", user.getEmail());
    }


    // CHANGE PASSWORD

    public void changePassword(User currentUser, AuthDtos.ChangePasswordRequest request) {

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new BusinessException("INVALID_PASSWORD", "La contraseña actual es incorrecta");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);

        refreshTokenRepository.revokeAllByUserId(currentUser.getId());

        auditService.log("AUTH", "CHANGE_PASSWORD",
            currentUser.getId(), currentUser.getEmail(),
            String.valueOf(currentUser.getId()), "Contraseña actualizada");
    }


    // ACTUALIZAR PERFIL (nombre / email)

    public AuthDtos.LoginResponse updateProfile(User user, AuthDtos.UpdateProfileRequest request) {
        String newEmail = request.getEmail().trim().toLowerCase();

        if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new DuplicateResourceException("Usuario", "email", newEmail);
        }

        user.setName(request.getName().trim());
        user.setEmail(newEmail);
        userRepository.save(user);

        auditService.log("AUTH", "UPDATE_PROFILE",
            user.getId(), user.getEmail(),
            String.valueOf(user.getId()), "Perfil actualizado (nombre/email)");

        return toProfileResponse(user);
    }

    // ACTUALIZAR FOTO DE PERFIL

    public AuthDtos.LoginResponse updateAvatar(User user, MultipartFile file) {
        String avatarUrl = avatarStorageService.store(file);
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        auditService.log("AUTH", "UPDATE_AVATAR",
            user.getId(), user.getEmail(),
            String.valueOf(user.getId()), "Foto de perfil actualizada");

        return toProfileResponse(user);
    }

    // No genera tokens nuevos — el front solo usa esta respuesta para
    // refrescar los datos del usuario en el store, la sesión sigue igual.
    private AuthDtos.LoginResponse toProfileResponse(User user) {
        return AuthDtos.LoginResponse.builder()
            .userId(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .role(user.getRole().getName())
            .avatarUrl(user.getAvatarUrl())
            .build();
    }


    // FORGOT PASSWORD
    //
    // Responde igual exista o no el email: así nadie puede usar este endpoint
    // para averiguar qué correos están registrados en el sistema.

    public void forgotPassword(AuthDtos.ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        userRepository.findByEmail(email)
            .filter(user -> user.getStatus() == User.Status.ACTIVE)
            .ifPresentOrElse(
                this::sendPasswordResetLink,
                () -> log.info("Recuperación solicitada para un email no registrado o inactivo: {}", email));
    }

    private void sendPasswordResetLink(User user) {
        // Los enlaces anteriores sin usar dejan de servir
        passwordResetTokenRepository.invalidateAllByUserId(user.getId(), LocalDateTime.now());

        // 32 bytes aleatorios = 256 bits: imposible de adivinar
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(sha256(token));
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(resetExpirationMinutes));
        passwordResetTokenRepository.save(resetToken);

        String link = frontendUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), link, resetExpirationMinutes);

        auditService.log("AUTH", "FORGOT_PASSWORD",
            user.getId(), user.getEmail(),
            String.valueOf(user.getId()), "Enlace de recuperación de contraseña generado");
    }

    // RESET PASSWORD (con el token del correo)

    public void resetPassword(AuthDtos.ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository
            .findByTokenHash(sha256(request.getToken()))
            .orElseThrow(() -> new BusinessException("INVALID_RESET_TOKEN",
                "El enlace de recuperación no es válido. Solicita uno nuevo."));

        if (resetToken.isUsed()) {
            throw new BusinessException("INVALID_RESET_TOKEN",
                "Este enlace ya fue usado. Solicita uno nuevo.");
        }
        if (resetToken.isExpired()) {
            throw new BusinessException("INVALID_RESET_TOKEN",
                "El enlace de recuperación venció. Solicita uno nuevo.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        // Cierra las sesiones abiertas: si alguien más tenía acceso, lo pierde
        refreshTokenRepository.revokeAllByUserId(user.getId());

        auditService.log("AUTH", "RESET_PASSWORD",
            user.getId(), user.getEmail(),
            String.valueOf(user.getId()), "Contraseña restablecida con enlace de recuperación");
    }

    // Se guarda el hash del token, no el token: si alguien lee la tabla, no puede usarlos
    private static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }


    // PRIVATE

    private void saveRefreshToken(User user, String tokenValue) {

        RefreshToken token = new RefreshToken();

        token.setToken(tokenValue);
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusSeconds(
            jwtProperties.getRefreshExpiration() / 1000));

        refreshTokenRepository.save(token);
    }
}