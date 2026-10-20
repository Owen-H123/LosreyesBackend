package com.delosreyes.inventario.modules.categories.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {

    private CategoryDtos() {}

    public static class Request {
        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        private String name;

        @Size(max = 255, message = "La descripción no debe superar 255 caracteres")
        private String description;

        private boolean active = true;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    public static class Response {
        private Long id;
        private String name;
        private String description;
        private boolean active;
        private LocalDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}