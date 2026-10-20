package com.delosreyes.inventario.shared.exceptions;

/**
 * DuplicateResourceException — Se lanza cuando se intenta crear un recurso que ya existe.
 *
 * Casos de uso:
 * - Email de usuario duplicado
 * - Código de producto duplicado
 * - Documento de cliente duplicado
 *
 * Resulta en HTTP 409 Conflict vía GlobalExceptionHandler.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s ya existe con %s: '%s'", resourceName, fieldName, fieldValue));
    }

    public DuplicateResourceException(String message) {
        super(message);
    }
}