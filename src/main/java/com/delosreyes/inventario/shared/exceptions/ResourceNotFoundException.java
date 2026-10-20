package com.delosreyes.inventario.shared.exceptions;

/**
 * ResourceNotFoundException — Se lanza cuando un recurso buscado no existe en BD.
 *
 * Casos de uso:
 * - Product no encontrado por ID
 * - User no encontrado por email
 * - Sale no encontrada
 *
 * Resulta en HTTP 404 Not Found vía GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s no encontrado con %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }

    public String getResourceName() { return resourceName; }
    public String getFieldName() { return fieldName; }
    public Object getFieldValue() { return fieldValue; }
}