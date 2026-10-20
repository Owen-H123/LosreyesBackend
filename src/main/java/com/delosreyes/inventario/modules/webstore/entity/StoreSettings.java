package com.delosreyes.inventario.modules.webstore.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** Ajustes de la tienda online. Siempre existe una sola fila (id = 1). */
@Entity
@Table(name = "store_settings")
public class StoreSettings {

    public static final short SINGLETON_ID = 1;

    @Id
    private Short id = SINGLETON_ID;

    @Column(length = 20)
    private String whatsapp;

    @Column(length = 200)
    private String announcement;

    /** Medios de pago separados por coma. */
    @Column(name = "payment_methods", nullable = false, length = 400)
    private String paymentMethods = "";

    @Column(name = "pickup_address", length = 255)
    private String pickupAddress;

    @Column(name = "business_hours", length = 150)
    private String businessHours;

    @Column(name = "shipping_note", length = 255)
    private String shippingNote;

    @Column(name = "yape_number", length = 20)
    private String yapeNumber;

    @Column(name = "yape_holder", length = 100)
    private String yapeHolder;

    @Column(name = "yape_qr_url", length = 500)
    private String yapeQrUrl;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    void touch() { updatedAt = LocalDateTime.now(); }

    public Short getId() { return id; }
    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }
    public String getAnnouncement() { return announcement; }
    public void setAnnouncement(String announcement) { this.announcement = announcement; }
    public String getPaymentMethods() { return paymentMethods; }
    public void setPaymentMethods(String paymentMethods) { this.paymentMethods = paymentMethods; }
    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }
    public String getBusinessHours() { return businessHours; }
    public void setBusinessHours(String businessHours) { this.businessHours = businessHours; }
    public String getShippingNote() { return shippingNote; }
    public void setShippingNote(String shippingNote) { this.shippingNote = shippingNote; }
    public String getYapeNumber() { return yapeNumber; }
    public void setYapeNumber(String yapeNumber) { this.yapeNumber = yapeNumber; }
    public String getYapeHolder() { return yapeHolder; }
    public void setYapeHolder(String yapeHolder) { this.yapeHolder = yapeHolder; }
    public String getYapeQrUrl() { return yapeQrUrl; }
    public void setYapeQrUrl(String yapeQrUrl) { this.yapeQrUrl = yapeQrUrl; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
