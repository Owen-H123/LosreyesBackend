package com.delosreyes.inventario.modules.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.delosreyes.inventario.modules.auth.dto.AuthDtos;
import com.delosreyes.inventario.modules.auth.service.AuthService;
import com.delosreyes.inventario.modules.users.entity.User;
import com.delosreyes.inventario.shared.exceptions.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * AuthController — Login, sesión, perfil y recuperación de contraseña.
 *
 * Las rutas que necesitan al usuario reciben @AuthenticationPrincipal User:
 * JwtAuthenticationFilter carga el User real en el SecurityContext a partir
 * del token, igual que en el resto de los controllers.
 *
 * El refresh token viaja en una cookie HttpOnly (ver RefreshTokenCookie), no
 * en el JSON: login la crea, refresh la lee y logout la borra.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookie refreshTokenCookie;

    public AuthController(AuthService authService, RefreshTokenCookie refreshTokenCookie) {
        this.authService = authService;
        this.refreshTokenCookie = refreshTokenCookie;
    }

    // ============================================================
    // LOGIN
    // ============================================================
    @PostMapping("/login")
    public ResponseEntity<AuthDtos.LoginResponse> login(
            @Valid @RequestBody AuthDtos.LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = httpRequest.getRemoteAddr();
        AuthDtos.LoginResponse response = authService.login(request, ipAddress);

        // El refresh token va en la cookie; en el JSON se omite (@JsonIgnore)
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.create(response.getRefreshToken()).toString())
            .body(response);
    }

    // ============================================================
    // REFRESH TOKEN
    // ============================================================
    @PostMapping("/refresh")
    public ResponseEntity<AuthDtos.LoginResponse> refresh(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Sesión vencida. Inicie sesión nuevamente.");
        }
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    // ============================================================
    // LOGOUT
    // ============================================================
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal User user) {
        authService.logout(user);
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear().toString())
            .build();
    }

    // ============================================================
    // CHANGE PASSWORD
    // ============================================================
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AuthDtos.ChangePasswordRequest request) {

        authService.changePassword(user, request);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // FORGOT PASSWORD
    // ============================================================
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {

        authService.forgotPassword(request);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // RESET PASSWORD (con el enlace del correo)
    // ============================================================
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody AuthDtos.ResetPasswordRequest request) {

        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // ACTUALIZAR MI PERFIL (nombre / email)
    // ============================================================
    @PutMapping("/profile")
    public ResponseEntity<AuthDtos.LoginResponse> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AuthDtos.UpdateProfileRequest request) {

        return ResponseEntity.ok(authService.updateProfile(user, request));
    }

    // ============================================================
    // SUBIR FOTO DE PERFIL
    // ============================================================
    @PostMapping("/profile/avatar")
    public ResponseEntity<AuthDtos.LoginResponse> uploadAvatar(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(authService.updateAvatar(user, file));
    }
}