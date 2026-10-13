package com.facturapro.users;

import java.util.EnumSet;
import java.util.Set;

/**
 * Roles del sistema. Los permisos se aplican en el backend con @PreAuthorize
 * (no se trata solo de ocultar botones en el frontend).
 */
public enum Role {

    ADMINISTRADOR(EnumSet.allOf(Permission.class)),

    GERENTE(EnumSet.complementOf(EnumSet.of(Permission.USERS_WRITE))),

    VENDEDOR(EnumSet.of(
            Permission.DASHBOARD_VIEW,
            Permission.PRODUCTS_VIEW,
            Permission.CUSTOMERS_VIEW,
            Permission.CUSTOMERS_WRITE,
            Permission.SALES_VIEW,
            Permission.SALES_WRITE,
            Permission.PAYMENTS_VIEW,
            Permission.PAYMENTS_WRITE,
            Permission.REPORTS_VIEW,
            Permission.REPORTS_EXPORT)),

    BODEGA(EnumSet.of(
            Permission.DASHBOARD_VIEW,
            Permission.PRODUCTS_VIEW,
            Permission.PRODUCTS_WRITE,
            Permission.SUPPLIERS_VIEW,
            Permission.SUPPLIERS_WRITE,
            Permission.INVENTORY_VIEW,
            Permission.INVENTORY_ADJUST,
            Permission.PURCHASES_VIEW,
            Permission.PURCHASES_WRITE,
            Permission.REPORTS_VIEW));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions() {
        return Set.copyOf(permissions);
    }

    public boolean has(Permission permission) {
        return permissions.contains(permission);
    }

    public String label() {
        return switch (this) {
            case ADMINISTRADOR -> "Administrador";
            case GERENTE -> "Gerente";
            case VENDEDOR -> "Vendedor";
            case BODEGA -> "Bodega";
        };
    }
}
