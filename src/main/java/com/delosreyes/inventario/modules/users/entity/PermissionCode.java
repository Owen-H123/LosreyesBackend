package com.delosreyes.inventario.modules.users.entity;

/**
 * PermissionCode — Catálogo fijo de permisos delegables a trabajadores.
 *
 * QUÉ HACE:
 *   Define las acciones que un ADMIN puede activar/desactivar individualmente
 *   para un WORKER de confianza, por encima de lo que le da su rol base.
 *
 * IMPORTANTE:
 *   MANAGE_USERS (crear/editar/suspender usuarios y otorgar estos mismos
 *   permisos) NO está aquí a propósito: es exclusivo de ADMIN y no se puede
 *   delegar, para evitar que un trabajador se auto-otorgue privilegios o
 *   cree cuentas ADMIN nuevas.
 */
public enum PermissionCode {
    VIEW_AUDIT,
    VIEW_REPORTS,
    ADJUST_INVENTORY,
    DELETE_PRODUCTS,
    CANCEL_SALES,
    MANAGE_CATEGORIES,
    EDIT_SALES
}