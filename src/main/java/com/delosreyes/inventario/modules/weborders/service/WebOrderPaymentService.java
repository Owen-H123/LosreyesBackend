package com.delosreyes.inventario.modules.weborders.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.OrderResponse;
import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.PaymentInfo;
import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.SubmitPaymentRequest;
import com.delosreyes.inventario.modules.weborders.entity.WebOrder;
import com.delosreyes.inventario.modules.weborders.entity.WebOrder.PaymentStatus;
import com.delosreyes.inventario.modules.weborders.repository.WebOrderRepository;
import com.delosreyes.inventario.shared.exceptions.BusinessException;
import com.delosreyes.inventario.shared.exceptions.ResourceNotFoundException;

/**
 * Pago de un pedido web (Yape).
 *
 * FLUJO:  UNPAID → (cliente envía el código) SUBMITTED → (el equipo verifica) PAID | REJECTED
 *         Un pago REJECTED permite al cliente enviar otro código.
 *
 * SEGURIDAD:
 *   - Las rutas del cliente son públicas, así que el número de pedido solo no basta:
 *     hay que conocer también el celular con el que se hizo.
 *   - Un código de operación no puede respaldar dos pedidos (índice único en la BD).
 *   - La pasarela es manual: se verifica contra los Yape recibidos. Para cobrar de forma
 *     automática habría que reemplazar {@link #submit} por el cobro del proveedor elegido.
 */
@Service
public class WebOrderPaymentService {

    private static final List<PaymentStatus> CODE_IN_USE = List.of(PaymentStatus.SUBMITTED, PaymentStatus.PAID);
    private static final String CODE_USED_MESSAGE = "Ese código de operación ya fue usado en otro pedido";

    private final WebOrderRepository orders;

    public WebOrderPaymentService(WebOrderRepository orders) {
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public PaymentInfo info(String orderNumber, String phone) {
        return PaymentInfo.from(ownedOrder(orderNumber, phone));
    }

    @Transactional
    public PaymentInfo submit(String orderNumber, SubmitPaymentRequest req) {
        WebOrder o = ownedOrder(orderNumber, req.phone());
        if (o.getStatus() == WebOrder.Status.CANCELLED) {
            throw new BusinessException("Este pedido fue cancelado");
        }
        if (o.getPaymentStatus() == PaymentStatus.PAID || o.getPaymentStatus() == PaymentStatus.SUBMITTED) {
            throw new BusinessException("Este pedido ya tiene un pago registrado");
        }
        String code = req.operationCode();
        if (orders.existsByPaymentOperationCodeAndPaymentStatusIn(code, CODE_IN_USE)) {
            throw new BusinessException(CODE_USED_MESSAGE);
        }

        o.setPaymentOperationCode(code);
        o.setPaymentStatus(PaymentStatus.SUBMITTED);
        o.setPaymentSubmittedAt(LocalDateTime.now());
        o.setPaymentVerifiedAt(null);
        try {
            orders.saveAndFlush(o);
        } catch (DataIntegrityViolationException e) {
            // Dos pedidos enviaron el mismo código a la vez: gana el primero
            throw new BusinessException(CODE_USED_MESSAGE);
        }
        return PaymentInfo.from(o);
    }

    @Transactional
    public OrderResponse verify(Long id, PaymentStatus decision) {
        WebOrder o = orders.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pedido web", "id", id));
        if (decision != PaymentStatus.PAID && decision != PaymentStatus.REJECTED) {
            throw new BusinessException("Solo se puede marcar como pagado o rechazado");
        }
        if (o.getPaymentStatus() != PaymentStatus.SUBMITTED) {
            throw new BusinessException("El cliente aún no informó ningún pago para verificar");
        }
        o.setPaymentStatus(decision);
        o.setPaymentVerifiedAt(LocalDateTime.now());
        return OrderResponse.from(o);
    }

    private WebOrder ownedOrder(String orderNumber, String phone) {
        WebOrder o = orders.findByOrderNumber(orderNumber).orElse(null);
        if (o == null || phone == null || !lastDigits(o.getPhone()).equals(lastDigits(phone))) {
            // Mismo mensaje en ambos casos: no se revela si el número de pedido existe
            throw new ResourceNotFoundException("Pedido web", "número", orderNumber);
        }
        return o;
    }

    private static String lastDigits(String phone) {
        String d = phone.replaceAll("\\D", "");
        return d.length() > 9 ? d.substring(d.length() - 9) : d;
    }
}
