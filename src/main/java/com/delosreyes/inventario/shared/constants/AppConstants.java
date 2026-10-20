package com.delosreyes.inventario.shared.constants;

/**
 * AppConstants — Constantes globales del sistema.
 *
 * QUÉ HACE:
 *   Centraliza todos los valores constantes usados en múltiples módulos.
 *
 * POR QUÉ EXISTE:
 *   Evitar "magic numbers" y strings duplicados en el código.
 *   Un solo lugar para cambiar valores que afectan todo el sistema.
 */
public final class AppConstants {

    private AppConstants() {}

    // Roles
    public static final String ROLE_ADMIN  = "ADMIN";
    public static final String ROLE_WORKER = "WORKER";

    // Paginación
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE   = 20;
    public static final int MAX_PAGE_SIZE       = 100;
    public static final String DEFAULT_SORT_BY  = "createdAt";
    public static final String DEFAULT_SORT_DIR = "desc";

    // Caché names
    public static final String CACHE_PRODUCTS   = "products";
    public static final String CACHE_CATEGORIES = "categories";
    public static final String CACHE_DASHBOARD  = "dashboard";
    public static final String CACHE_VEHICLES   = "vehicles";

    // Módulos de auditoría
    public static final String MODULE_AUTH      = "AUTH";
    public static final String MODULE_USERS     = "USERS";
    public static final String MODULE_PRODUCTS  = "PRODUCTS";
    public static final String MODULE_SALES     = "SALES";
    public static final String MODULE_INVENTORY = "INVENTORY";
    public static final String MODULE_CUSTOMERS = "CUSTOMERS";

    // Acciones de auditoría
    public static final String ACTION_LOGIN     = "LOGIN";
    public static final String ACTION_LOGOUT    = "LOGOUT";
    public static final String ACTION_CREATE    = "CREATE";
    public static final String ACTION_UPDATE    = "UPDATE";
    public static final String ACTION_DELETE    = "DELETE";
    public static final String ACTION_VIEW      = "VIEW";

    // Mensajes de respuesta comunes
    public static final String MSG_CREATED       = "Registro creado exitosamente";
    public static final String MSG_UPDATED       = "Registro actualizado exitosamente";
    public static final String MSG_DELETED       = "Registro eliminado exitosamente";
    public static final String MSG_FOUND         = "Registro encontrado";
    public static final String MSG_LIST          = "Lista obtenida exitosamente";
    public static final String MSG_LOGIN_SUCCESS = "Sesión iniciada exitosamente";
    public static final String MSG_LOGOUT        = "Sesión cerrada exitosamente";

    // Stock
    public static final int LOW_STOCK_THRESHOLD = 5;

    // Sale prefix
    public static final String SALE_PREFIX = "VTA";
}