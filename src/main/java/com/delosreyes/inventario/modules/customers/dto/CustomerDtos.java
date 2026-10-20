package com.delosreyes.inventario.modules.customers.dto;

import java.time.LocalDateTime;

import com.delosreyes.inventario.modules.customers.entity.Customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CustomerDtos {

    private CustomerDtos() {}

    public static class Request {
        @NotNull(message = "El tipo de documento es obligatorio")
        private Customer.DocumentType documentType = Customer.DocumentType.DNI;

        @NotBlank(message = "El número de documento es obligatorio")
        @Size(min = 8, max = 20, message = "El documento debe tener entre 8 y 20 caracteres")
        private String documentNumber;

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 200)
        private String name;

        @Email(message = "Formato de email inválido")
        private String email;

        @Size(max = 20)
        private String phone;

        @Size(max = 500)
        private String address;

        public Customer.DocumentType getDocumentType() { return documentType; }
        public void setDocumentType(Customer.DocumentType documentType) { this.documentType = documentType; }
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
    }

    public static class Response {
        private Long id;
        private Customer.DocumentType documentType;
        private String documentNumber;
        private String name;
        private String email;
        private String phone;
        private String address;
        private boolean active;
        private LocalDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Customer.DocumentType getDocumentType() { return documentType; }
        public void setDocumentType(Customer.DocumentType documentType) { this.documentType = documentType; }
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
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}