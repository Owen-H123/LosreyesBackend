package com.delosreyes.inventario.shared.exceptions;

/**
 * UnauthorizedException — Se lanza cuando un usuario no tiene permisos para una acción.
 * Resulta en HTTP 403 Forbidden vía GlobalExceptionHandler.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException() {
        super("No tiene permisos para realizar esta acción");
    }
}