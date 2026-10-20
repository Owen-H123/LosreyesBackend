package com.delosreyes.inventario.modules.vehicles.entity;

import java.util.HashSet;
import java.util.Set;

import com.delosreyes.inventario.modules.products.entity.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Vehicle — Vehículo compatible con productos (Peugeot, Citroën, Renault).
 *
 * RELACIONES:
 *   ManyToMany con Product a través de la tabla product_vehicles.
 *   Un vehículo puede ser compatible con múltiples productos.
 *   Un producto puede ser compatible con múltiples vehículos.
 */
@Entity
@Table(
    name = "vehicles",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"brand", "model", "manufacture_year", "engine"}
    )
)
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String brand;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "manufacture_year", nullable = false)
    private Integer year;

    @Column(length = 50)
    private String engine;

    /**
     * Productos compatibles con este vehículo.
     * La tabla intermedia es administrada por Product.
     */
    @ManyToMany(mappedBy = "vehicles")
    private Set<Product> products = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public Set<Product> getProducts() {
        return products;
    }

    public void setProducts(Set<Product> products) {
        this.products = products;
    }
}