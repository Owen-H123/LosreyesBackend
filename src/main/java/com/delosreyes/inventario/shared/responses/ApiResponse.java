package com.delosreyes.inventario.shared.responses;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * ApiResponse<T> — Envoltorio estándar para todas las respuestas de la API.
 *
 * QUÉ HACE:
 *   Garantiza que cada endpoint devuelva siempre la misma estructura JSON,
 *   independientemente del módulo o tipo de dato retornado.
 *
 * POR QUÉ EXISTE:
 *   El frontend React necesita un contrato estable para parsear respuestas.
 *   Evita que cada controller invente su propio formato de respuesta.
 *
 * ESTRUCTURA:
 * {
 *   "success": true,
 *   "message": "Operación exitosa",
 *   "data": { ... },
 *   "errors": [],
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 *
 * USO:
 *   return ResponseEntity.ok(ApiResponse.success("Producto creado", productDTO));
 *   return ResponseEntity.badRequest().body(ApiResponse.error("Código duplicado"));
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private List<String> errors;
    private LocalDateTime timestamp;

    private ApiResponse() {
        this.timestamp = LocalDateTime.now();
        this.errors = new ArrayList<>();
    }

    // Factory methods — construcción semántica de respuestas

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = message;
        response.data = data;
        return response;
    }

    public static <T> ApiResponse<T> success(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = message;
        return response;
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        return response;
    }

    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        response.errors = errors;
        return response;
    }

    // Getters (sin Lombok — Java Records no aplicable por mutabilidad)


    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public List<String> getErrors() { return errors; }
    public LocalDateTime getTimestamp() { return timestamp; }
}