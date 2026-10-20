package com.delosreyes.inventario.modules.weborders.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.OrderFilter;
import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.OrderResponse;
import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.StatusRequest;
import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.Summary;
import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.TopProduct;
import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.VerifyPaymentRequest;
import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.WebCustomer;
import com.delosreyes.inventario.modules.weborders.entity.WebOrder;
import com.delosreyes.inventario.modules.weborders.service.WebOrderPaymentService;
import com.delosreyes.inventario.modules.weborders.service.WebOrderService;
import com.delosreyes.inventario.shared.responses.ApiResponse;
import com.delosreyes.inventario.shared.responses.PagedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Gestión de pedidos web desde el ERP (requiere login: ADMIN o WORKER). */
@RestController
@RequestMapping("/web-orders")
@Tag(name = "Pedidos web", description = "Gestión de los pedidos de la tienda online")
public class WebOrderController {

    private final WebOrderService orders;
    private final WebOrderPaymentService payments;

    public WebOrderController(WebOrderService orders, WebOrderPaymentService payments) {
        this.orders = orders;
        this.payments = payments;
    }

    @GetMapping
    @Operation(summary = "Listar pedidos web")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> list(
            @RequestParam(required = false) List<WebOrder.Status> status,
            @RequestParam(required = false) WebOrder.Delivery delivery,
            @RequestParam(required = false) WebOrder.PaymentStatus paymentStatus,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var filter = new OrderFilter(status, delivery, paymentStatus, search, from, to);
        return ResponseEntity.ok(ApiResponse.success("Pedidos", orders.list(filter, page, size)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumen del panel de la tienda")
    public ResponseEntity<ApiResponse<Summary>> summary() {
        return ResponseEntity.ok(ApiResponse.success("Resumen", orders.summary()));
    }

    @GetMapping("/customers")
    @Operation(summary = "Clientes de la tienda online, agrupados por celular")
    public ResponseEntity<ApiResponse<List<WebCustomer>>> customers() {
        return ResponseEntity.ok(ApiResponse.success("Clientes web", orders.customers()));
    }

    @GetMapping("/top-products")
    @Operation(summary = "Productos más pedidos desde la web en los últimos 30 días")
    public ResponseEntity<ApiResponse<List<TopProduct>>> topProducts() {
        return ResponseEntity.ok(ApiResponse.success("Más pedidos", orders.topProducts()));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambiar el estado de un pedido web")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Estado actualizado", orders.updateStatus(id, req.status())));
    }

    @PatchMapping("/{id}/payment")
    @Operation(summary = "Verificar o rechazar el pago que informó el cliente")
    public ResponseEntity<ApiResponse<OrderResponse>> verifyPayment(@PathVariable Long id, @Valid @RequestBody VerifyPaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Pago actualizado", payments.verify(id, req.status())));
    }
}
