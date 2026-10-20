package com.delosreyes.inventario.shared.exceptions;

/**
 * BusinessException — Se lanza cuando una regla de negocio es violada.
 *
 * Casos de uso:
 * - Stock insuficiente al registrar venta
 * - Intentar vender un producto inactivo
 * - Código de producto duplicado
 * - Precio negativo
 *
 * Resulta en HTTP 422 Unprocessable Entity vía GlobalExceptionHandler.
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}