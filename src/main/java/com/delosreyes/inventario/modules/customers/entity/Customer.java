package com.delosreyes.inventario.modules.customers.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Customer — Cliente que realiza compras en la tienda.
 *
 * POR QUÉ EXISTE:
 *   Permite asociar ventas a clientes para historial y reportes.
 *   Una venta puede ser a cliente identificado o sin identificar (público general).
 */
@Entity
@Table(name = "customers")
public class Customer {

    public enum DocumentType { DNI, RUC, CE, PASSPORT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 10)
    private DocumentType documentType = DocumentType.DNI;

    @Column(name = "document_number", nullable = false, unique = true, length = 20)
    private String documentNumber;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}