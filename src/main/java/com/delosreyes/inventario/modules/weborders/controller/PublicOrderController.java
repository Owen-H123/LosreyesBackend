package com.delosreyes.inventario.modules.weborders.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.CreateRequest;
import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.OrderResponse;
import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.PaymentInfo;
import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.SubmitPaymentRequest;
import com.delosreyes.inventario.modules.weborders.service.WebOrderPaymentService;
import com.delosreyes.inventario.modules.weborders.service.WebOrderService;
import com.delosreyes.inventario.shared.responses.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Lo que usa el cliente de la tienda online, sin login (ver SecurityConfig).
 *   POST /public/orders                  → enviar el pedido
 *   GET  /public/orders/{n}/payment      → estado del pago (celular en X-Order-Phone)
 *   POST /public/orders/{n}/payment      → informar el pago con Yape
 */
@RestController
@RequestMapping("/public/orders")
@Tag(name = "Pedidos web (público)", description = "Pedido y pago desde la tienda online")
public class PublicOrderController {

    private final WebOrderService orders;
    private final WebOrderPaymentService payments;

    public PublicOrderController(WebOrderService orders, WebOrderPaymentService payments) {
        this.orders = orders;
        this.payments = payments;
    }

    @PostMapping
    @Operation(summary = "Registrar un pedido desde la tienda online")
    public ResponseEntity<ApiResponse<OrderResponse>> create(@Valid @RequestBody CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Pedido registrado", orders.create(req)));
    }

    @GetMapping("/{orderNumber}/payment")
    @Operation(summary = "Estado de pago de un pedido")
    public ResponseEntity<ApiResponse<PaymentInfo>> paymentInfo(
            @PathVariable String orderNumber,
            @RequestHeader(value = "X-Order-Phone", required = false) String phone) {
        return ResponseEntity.ok(ApiResponse.success("Pago", payments.info(orderNumber, phone)));
    }

    @PostMapping("/{orderNumber}/payment")
    @Operation(summary = "El cliente informa que pagó con Yape y registra su código de operación")
    public ResponseEntity<ApiResponse<PaymentInfo>> submitPayment(
            @PathVariable String orderNumber, @Valid @RequestBody SubmitPaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Pago registrado", payments.submit(orderNumber, req)));
    }
}
