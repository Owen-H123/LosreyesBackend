package com.delosreyes.inventario.modules.weborders.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** Pedido hecho desde la tienda online. Ver V9__add_web_orders.sql. */
@Entity
@Table(name = "web_orders")
public class WebOrder {

    public enum Delivery { SHIPPING, PICKUP }
    public enum Status { NEW, CONFIRMED, SHIPPED, DELIVERED, CANCELLED }
    /** UNPAID: sin pago · SUBMITTED: el cliente envió su código · PAID: verificado · REJECTED: el código no cuadró. */
    public enum PaymentStatus { UNPAID, SUBMITTED, PAID, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 20)
    private String orderNumber;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Delivery delivery;

    private String address;
    private String district;
    private String car;

    @Column(name = "payment_method", nullable = false, length = 40)
    private String paymentMethod;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Status status = Status.NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 12)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "payment_operation_code", length = 20)
    private String paymentOperationCode;

    @Column(name = "payment_submitted_at")
    private LocalDateTime paymentSubmittedAt;

    @Column(name = "payment_verified_at")
    private LocalDateTime paymentVerifiedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<WebOrderItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public void addItem(WebOrderItem item) { item.setOrder(this); items.add(item); }

    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Delivery getDelivery() { return delivery; }
    public void setDelivery(Delivery delivery) { this.delivery = delivery; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getCar() { return car; }
    public void setCar(String car) { this.car = car; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPaymentOperationCode() { return paymentOperationCode; }
    public void setPaymentOperationCode(String paymentOperationCode) { this.paymentOperationCode = paymentOperationCode; }
    public LocalDateTime getPaymentSubmittedAt() { return paymentSubmittedAt; }
    public void setPaymentSubmittedAt(LocalDateTime paymentSubmittedAt) { this.paymentSubmittedAt = paymentSubmittedAt; }
    public LocalDateTime getPaymentVerifiedAt() { return paymentVerifiedAt; }
    public void setPaymentVerifiedAt(LocalDateTime paymentVerifiedAt) { this.paymentVerifiedAt = paymentVerifiedAt; }
    public List<WebOrderItem> getItems() { return items; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
