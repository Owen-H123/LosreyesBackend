package com.delosreyes.inventario.modules.auth.service;

import com.delosreyes.inventario.config.JwtProperties;
import com.delosreyes.inventario.modules.audit.service.AuditService;
import com.delosreyes.inventario.modules.auth.dto.AuthDtos;
import com.delosreyes.inventario.modules.auth.entity.PasswordResetToken;
import com.delosreyes.inventario.modules.auth.repository.PasswordResetTokenRepository;
import com.delosreyes.inventario.modules.auth.repository.RefreshTokenRepository;
import com.delosreyes.inventario.modules.receipts.service.EmailService;
import com.delosreyes.inventario.modules.users.entity.User;
import com.delosreyes.inventario.modules.users.repository.UserRepository;
import com.delosreyes.inventario.modules.users.service.AvatarStorageService;
import com.delosreyes.inventario.security.service.JwtService;
import com.delosreyes.inventario.shared.exceptions.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la recuperación de contraseña por correo.
 */
class AuthServicePasswordResetTest {

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordResetTokenRepository resetTokenRepository;
    private EmailService emailService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        resetTokenRepository = mock(PasswordResetTokenRepository.class);
        emailService = mock(EmailService.class);

        authService = new AuthService(mock(AuthenticationManager.class), userRepository,
            refreshTokenRepository, mock(JwtService.class), mock(JwtProperties.class),
            passwordEncoder, mock(AuditService.class), mock(AvatarStorageService.class),
            resetTokenRepository, emailService, "http://localhost:3000", 30);

        user = new User();
        user.setId(5L);
        user.setName("Cajero");
        user.setEmail("cajero@delosreyes.com");
        user.setPassword(passwordEncoder.encode("ClaveVieja1"));
        user.setStatus(User.Status.ACTIVE);

        when(userRepository.findByEmail("cajero@delosreyes.com")).thenReturn(Optional.of(user));
        when(resetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void usuarioActivoRecibeUnEnlaceYSoloSeGuardaElHashDelToken() {
        authService.forgotPassword(forgot("  Cajero@DelosReyes.com "));

        String token = tokenFromSentLink();
        PasswordResetToken saved = savedResetToken();

        assertThat(token).hasSizeGreaterThanOrEqualTo(43);           // 256 bits en base64
        assertThat(saved.getTokenHash()).hasSize(64).isNotEqualTo(token);
        assertThat(saved.getExpiresAt()).isBetween(
            LocalDateTime.now().plusMinutes(29), LocalDateTime.now().plusMinutes(31));
        verify(resetTokenRepository).invalidateAllByUserId(eq(5L), any());
    }

    @Test
    void emailNoRegistradoNoEnviaNadaYNoDaError() {
        authService.forgotPassword(forgot("nadie@delosreyes.com"));

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString(), anyInt());
        verify(resetTokenRepository, never()).save(any());
    }

    @Test
    void usuarioInactivoNoRecibeEnlace() {
        user.setStatus(User.Status.INACTIVE);

        authService.forgotPassword(forgot("cajero@delosreyes.com"));

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void conElEnlaceSeCambiaLaContrasenaYSeCierranLasSesiones() {
        String token = requestLink();

        authService.resetPassword(reset(token, "ClaveNueva123"));

        assertThat(passwordEncoder.matches("ClaveNueva123", user.getPassword())).isTrue();
        assertThat(savedResetToken().isUsed()).isTrue();
        verify(refreshTokenRepository).revokeAllByUserId(5L);
    }

    @Test
    void elEnlaceNoSePuedeUsarDosVeces() {
        String token = requestLink();
        authService.resetPassword(reset(token, "ClaveNueva123"));

        assertThatThrownBy(() -> authService.resetPassword(reset(token, "OtraClave456")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ya fue usado");
        assertThat(passwordEncoder.matches("ClaveNueva123", user.getPassword())).isTrue();
    }

    @Test
    void elEnlaceVencidoSeRechaza() {
        String token = requestLink();
        savedResetToken().setExpiresAt(LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> authService.resetPassword(reset(token, "ClaveNueva123")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("venció");
        assertThat(passwordEncoder.matches("ClaveVieja1", user.getPassword())).isTrue();
    }

    @Test
    void unTokenInventadoSeRechaza() {
        assertThatThrownBy(() -> authService.resetPassword(reset("token-inventado", "ClaveNueva123")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("no es válido");
    }

    // ------------------------------------------------------------------

    /** Pide el enlace y deja el token guardado disponible para buscarlo por su hash. */
    private String requestLink() {
        authService.forgotPassword(forgot("cajero@delosreyes.com"));
        PasswordResetToken saved = savedResetToken();
        when(resetTokenRepository.findByTokenHash(saved.getTokenHash())).thenReturn(Optional.of(saved));
        return tokenFromSentLink();
    }

    private String tokenFromSentLink() {
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("cajero@delosreyes.com"), eq("Cajero"), link.capture(), eq(30));
        assertThat(link.getValue()).startsWith("http://localhost:3000/reset-password?token=");
        return link.getValue().substring(link.getValue().indexOf("token=") + 6);
    }

    private PasswordResetToken savedResetToken() {
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(resetTokenRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getAllValues().get(0);
    }

    private AuthDtos.ForgotPasswordRequest forgot(String email) {
        AuthDtos.ForgotPasswordRequest request = new AuthDtos.ForgotPasswordRequest();
        request.setEmail(email);
        return request;
    }

    private AuthDtos.ResetPasswordRequest reset(String token, String newPassword) {
        AuthDtos.ResetPasswordRequest request = new AuthDtos.ResetPasswordRequest();
        request.setToken(token);
        request.setNewPassword(newPassword);
        return request;
    }
}
