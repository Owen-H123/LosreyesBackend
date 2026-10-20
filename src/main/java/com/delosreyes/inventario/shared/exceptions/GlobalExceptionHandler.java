package com.delosreyes.inventario.shared.exceptions;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.delosreyes.inventario.shared.responses.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ============================================================
    // 404 - Recurso no encontrado
    // ============================================================
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ============================================================
    // 409 - Duplicados
    // ============================================================
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(DuplicateResourceException ex) {
        log.warn("Recurso duplicado: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ============================================================
    // 422 - Reglas de negocio
    // ============================================================
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Error de negocio [{}]: {}", ex.getErrorCode(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ============================================================
    // 400 - Validaciones
    // ============================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException ex) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .sorted()
                .collect(Collectors.toList());

        log.warn("Errores de validación: {}", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error de validación", errors));
    }

    // ============================================================
    // 401 - Bad credentials
    // ============================================================
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {

        log.warn("Credenciales incorrectas: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Email o contraseña incorrectos"));
    }

    // ============================================================
    // 401 - Usuario no encontrado (🔥 NUEVO)
    // ============================================================
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UsernameNotFoundException ex) {

        log.warn("Usuario no encontrado: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Usuario o contraseña incorrectos"));
    }

    // ============================================================
    // 401 - Usuario desactivado
    // ============================================================
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledUser(DisabledException ex) {

        log.warn("Usuario desactivado: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Usuario desactivado. Contacte al administrador"));
    }

    // ============================================================
    // 423 - Usuario bloqueado
    // ============================================================
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLockedUser(LockedException ex) {

        log.warn("Usuario bloqueado: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(ApiResponse.error("Usuario bloqueado. Contacte al administrador"));
    }

    // ============================================================
    // 403 - Acceso denegado
    // ============================================================
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {

        log.warn("Acceso denegado: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tiene permisos para realizar esta acción"));
    }

    // ============================================================
    // 401 - Sesión inválida o vencida (refresh token)
    // ============================================================
    // 401 y no 403: el usuario no está autenticado. El frontend, ante un 401
    // al renovar la sesión, lo manda al login.
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ============================================================
    // 413 - Archivos grandes
    // ============================================================
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("El archivo excede el tamaño máximo permitido"));
    }

    // ============================================================
    // 404 - LA RUTA NO EXISTE
    // ============================================================
    // Sin este handler, el handler genérico lo convertía en 500.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("La ruta solicitada no existe"));
    }

    // ============================================================
    // 405 - MÉTODO HTTP NO PERMITIDO (ej: GET donde se espera POST)
    // ============================================================
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("Método " + ex.getMethod() + " no permitido en esta ruta"));
    }

    // ============================================================
    // 500 - ERROR INESPERADO
    // ============================================================
    // El detalle va al log. Al cliente se le devuelve un mensaje genérico: el
    // mensaje interno de la excepción puede exponer SQL, clases o rutas.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {

        log.error("Error interno no controlado:", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Ocurrió un error interno. Si persiste, contacta al administrador."));
    }
}