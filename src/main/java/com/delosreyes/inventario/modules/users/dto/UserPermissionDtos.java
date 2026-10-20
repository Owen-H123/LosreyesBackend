package com.delosreyes.inventario.modules.users.dto;

import java.util.List;

import com.delosreyes.inventario.modules.users.entity.PermissionCode;

public final class UserPermissionDtos {

    private UserPermissionDtos() {}

    // RESPONSE: estado de un permiso para un usuario
    public static class PermissionItem {
        private PermissionCode code;
        private String label;
        private boolean granted;

        public PermissionItem() {}
        public PermissionItem(PermissionCode code, String label, boolean granted) {
            this.code = code; this.label = label; this.granted = granted;
        }

        public PermissionCode getCode() { return code; }
        public void setCode(PermissionCode code) { this.code = code; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public boolean isGranted() { return granted; }
        public void setGranted(boolean granted) { this.granted = granted; }
    }

    // REQUEST: reemplaza el set completo de permisos otorgados
    public static class UpdateRequest {
        private List<PermissionCode> permissions;

        public List<PermissionCode> getPermissions() { return permissions; }
        public void setPermissions(List<PermissionCode> permissions) { this.permissions = permissions; }
    }
}