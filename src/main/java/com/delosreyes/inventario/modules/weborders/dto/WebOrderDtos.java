package com.delosreyes.inventario.modules.weborders.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.delosreyes.inventario.modules.weborders.entity.WebOrder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Entradas y salidas del módulo de pedidos web. Las entidades nunca salen por la API. */
public final class WebOrderDtos {

    private WebOrderDtos() {}

    public static final int MAX_ITEMS = 30;
    private static final String PHONE_REGEX = "^[0-9 +]{9,15}$";

    // ── Entradas ──────────────────────────────────────────────

    public record ItemRequest(@NotNull Long productId, @NotNull @Min(1) @Max(99) Integer quantity) {}

    public record CreateRequest(
        @NotBlank @Size(max = 150) String customerName,
        @NotBlank @Pattern(regexp = PHONE_REGEX, message = "Celular inválido") String phone,
        @NotNull WebOrder.Delivery delivery,
        @Size(max = 255) String address,
        @Size(max = 100) String district,
        @Size(max = 120) String car,
        @NotBlank @Size(max = 40) String paymentMethod,
        @NotEmpty @Size(max = MAX_ITEMS) List<@Valid ItemRequest> items) {}

    public record StatusRequest(@NotNull WebOrder.Status status) {}

    public record SubmitPaymentRequest(
        @NotBlank @Pattern(regexp = PHONE_REGEX, message = "Celular inválido") String phone,
        @NotBlank @Pattern(regexp = "^[0-9]{6,12}$", message = "El código de operación son de 6 a 12 dígitos") String operationCode) {}

    public record VerifyPaymentRequest(@NotNull WebOrder.PaymentStatus status) {}

    /** Filtros del listado de pedidos; todos opcionales. */
    public record OrderFilter(
        List<WebOrder.Status> status, WebOrder.Delivery delivery, WebOrder.PaymentStatus paymentStatus,
        String search, LocalDate from, LocalDate to) {}

    // ── Salidas ───────────────────────────────────────────────

    public record ItemResponse(Long productId, String productCode, String productName, BigDecimal unitPrice, Integer quantity) {}

    public record OrderResponse(
        Long id, String orderNumber, String customerName, String phone, WebOrder.Delivery delivery,
        String address, String district, String car, String paymentMethod, BigDecimal total,
        WebOrder.Status status, List<ItemResponse> items, LocalDateTime createdAt,
        WebOrder.PaymentStatus paymentStatus, String paymentOperationCode, LocalDateTime paymentSubmittedAt) {

        public static OrderResponse from(WebOrder o) {
            return new OrderResponse(o.getId(), o.getOrderNumber(), o.getCustomerName(), o.getPhone(),
                o.getDelivery(), o.getAddress(), o.getDistrict(), o.getCar(), o.getPaymentMethod(),
                o.getTotal(), o.getStatus(),
                o.getItems().stream().map(i -> new ItemResponse(i.getProductId(), i.getProductCode(),
                    i.getProductName(), i.getUnitPrice(), i.getQuantity())).toList(),
                o.getCreatedAt(), o.getPaymentStatus(), o.getPaymentOperationCode(), o.getPaymentSubmittedAt());
        }
    }

    /** Lo que ve el cliente en la pantalla de pago: sin dirección ni datos de otros pedidos. */
    public record PaymentInfo(String orderNumber, BigDecimal total, String paymentMethod,
                              WebOrder.Status status, WebOrder.PaymentStatus paymentStatus) {
        public static PaymentInfo from(WebOrder o) {
            return new PaymentInfo(o.getOrderNumber(), o.getTotal(), o.getPaymentMethod(), o.getStatus(), o.getPaymentStatus());
        }
    }

    public record WebCustomer(String phone, String name, long orders, BigDecimal totalSpent, LocalDateTime lastOrderAt) {}

    public record TopProduct(String code, String name, long units, BigDecimal amount) {}

    public record Summary(
        long newCount, long confirmedCount, long shippedCount, long monthCount, BigDecimal monthTotal,
        long publishedProducts, long paymentsToVerify, long deliveriesPending) {}
}
