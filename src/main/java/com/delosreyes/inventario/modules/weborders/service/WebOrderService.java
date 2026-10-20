package com.delosreyes.inventario.modules.weborders.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delosreyes.inventario.modules.products.entity.Product;
import com.delosreyes.inventario.modules.products.repository.ProductRepository;
import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.CreateRequest;
import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.ItemRequest;
import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.OrderFilter;
import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.OrderResponse;
import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.Summary;
import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.TopProduct;
import com.delosreyes.inventario.modules.weborders.dto.WebOrderDtos.WebCustomer;
import com.delosreyes.inventario.modules.weborders.entity.WebOrder;
import com.delosreyes.inventario.modules.weborders.entity.WebOrderItem;
import com.delosreyes.inventario.modules.weborders.repository.WebOrderRepository;
import com.delosreyes.inventario.shared.exceptions.BusinessException;
import com.delosreyes.inventario.shared.exceptions.ResourceNotFoundException;
import com.delosreyes.inventario.shared.responses.PagedResponse;

import jakarta.persistence.criteria.Predicate;

/**
 * Pedidos de la tienda online: registro desde la web y gestión desde el ERP.
 * El pago de cada pedido vive en {@link WebOrderPaymentService}.
 *
 * REGLAS:
 *   - El servidor calcula los precios; nunca se confía en los que manda el navegador.
 *   - Solo se pueden pedir productos activos, no eliminados y publicados en la web.
 *   - Un pedido web NO descuenta stock: eso ocurre al registrar la venta en el ERP.
 */
@Service
public class WebOrderService {

    private static final int MAX_PAGE_SIZE = 100;

    private final WebOrderRepository orders;
    private final ProductRepository products;

    public WebOrderService(WebOrderRepository orders, ProductRepository products) {
        this.orders = orders;
        this.products = products;
    }

    @Transactional
    public OrderResponse create(CreateRequest req) {
        if (req.delivery() == WebOrder.Delivery.SHIPPING
                && (isBlank(req.address()) || isBlank(req.district()))) {
            throw new BusinessException("Para envío a domicilio indica dirección y distrito");
        }

        WebOrder order = new WebOrder();
        order.setCustomerName(req.customerName().trim());
        order.setPhone(req.phone().trim());
        order.setDelivery(req.delivery());
        order.setAddress(trimOrNull(req.address()));
        order.setDistrict(trimOrNull(req.district()));
        order.setCar(trimOrNull(req.car()));
        order.setPaymentMethod(req.paymentMethod().trim());

        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest it : req.items()) {
            Product p = products.findById(it.productId())
                .filter(x -> !x.isDeleted() && x.isWebPublished() && x.getStatus() == Product.Status.ACTIVE)
                .orElseThrow(() -> new BusinessException("Un producto del carrito ya no está disponible"));
            if (p.getStock() < it.quantity()) {
                throw new BusinessException("Stock insuficiente para " + p.getName() + " (quedan " + p.getStock() + ")");
            }
            WebOrderItem item = new WebOrderItem();
            item.setProductId(p.getId());
            item.setProductCode(p.getCode());
            item.setProductName(p.getName());
            item.setUnitPrice(p.getSalePrice());
            item.setQuantity(it.quantity());
            order.addItem(item);
            total = total.add(p.getSalePrice().multiply(BigDecimal.valueOf(it.quantity())));
        }
        order.setTotal(total);
        order.setOrderNumber(String.format("WEB-%05d", orders.nextNumber()));

        return OrderResponse.from(orders.save(order));
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> list(OrderFilter f, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by("createdAt").descending());

        Specification<WebOrder> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (f.status() != null && !f.status().isEmpty()) ps.add(root.get("status").in(f.status()));
            if (f.delivery() != null) ps.add(cb.equal(root.get("delivery"), f.delivery()));
            if (f.paymentStatus() != null) ps.add(cb.equal(root.get("paymentStatus"), f.paymentStatus()));
            if (f.search() != null && !f.search().isBlank()) {
                String like = "%" + f.search().trim().toLowerCase() + "%";
                ps.add(cb.or(
                    cb.like(cb.lower(root.get("orderNumber")), like),
                    cb.like(cb.lower(root.get("customerName")), like),
                    cb.like(root.get("phone"), like)));
            }
            if (f.from() != null) ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.from().atStartOfDay()));
            if (f.to() != null) ps.add(cb.lessThan(root.get("createdAt"), f.to().plusDays(1).atStartOfDay()));
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Page<OrderResponse> result = orders.findAll(spec, pageable).map(OrderResponse::from);
        return PagedResponse.from(result);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, WebOrder.Status status) {
        WebOrder o = orders.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pedido web", "id", id));
        o.setStatus(status);
        return OrderResponse.from(o);
    }

    @Transactional(readOnly = true)
    public List<WebCustomer> customers() {
        return orders.customerStats().stream()
            .map(r -> new WebCustomer((String) r[0], (String) r[1], (Long) r[2], (BigDecimal) r[3], (LocalDateTime) r[4]))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TopProduct> topProducts() {
        return orders.topProductsSince(LocalDateTime.now().minusDays(30), PageRequest.of(0, 5)).stream()
            .map(r -> new TopProduct((String) r[0], (String) r[1], ((Number) r[2]).longValue(), (BigDecimal) r[3]))
            .toList();
    }

    @Transactional(readOnly = true)
    public Summary summary() {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long published = products.count((root, q, cb) -> cb.and(
            cb.isFalse(root.get("deleted")), cb.isTrue(root.get("webPublished")),
            cb.equal(root.get("status"), Product.Status.ACTIVE)));
        long deliveries = orders.count((root, q, cb) -> root.get("status").in(
            WebOrder.Status.CONFIRMED, WebOrder.Status.SHIPPED));

        return new Summary(
            orders.countByStatus(WebOrder.Status.NEW),
            orders.countByStatus(WebOrder.Status.CONFIRMED),
            orders.countByStatus(WebOrder.Status.SHIPPED),
            orders.countByCreatedAtGreaterThanEqual(monthStart),
            orders.sumTotalSince(monthStart),
            published,
            orders.countByPaymentStatus(WebOrder.PaymentStatus.SUBMITTED),
            deliveries);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String trimOrNull(String s) { return isBlank(s) ? null : s.trim(); }
}
