package com.delosreyes.inventario.modules.vehicles.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class VehicleDtos {

    private VehicleDtos() {}

    public static class Request {
        @NotBlank(message = "La marca es obligatoria")
        @Size(max = 50)
        private String brand;

        @NotBlank(message = "El modelo es obligatorio")
        @Size(max = 100)
        private String model;

        @NotNull(message = "El año es obligatorio")
        @Min(value = 1980, message = "Año mínimo: 1980")
        @Max(value = 2030, message = "Año máximo: 2030")
        private Integer year;

        @Size(max = 50)
        private String engine;

        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public String getEngine() { return engine; }
        public void setEngine(String engine) { this.engine = engine; }
    }

    public static class Response {
        private Long id;
        private String brand;
        private String model;
        private Integer year;
        private String engine;
        private String displayName;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public String getEngine() { return engine; }
        public void setEngine(String engine) { this.engine = engine; }
        public String getDisplayName() {
            return brand + " " + model + " " + year + (engine != null ? " " + engine : "");
        }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }
}