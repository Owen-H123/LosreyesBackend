package com.delosreyes.inventario.modules.sales.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delosreyes.inventario.modules.audit.service.AuditService;
import com.delosreyes.inventario.modules.customers.entity.Customer;
import com.delosreyes.inventario.modules.customers.service.CustomerService;
import com.delosreyes.inventario.modules.movements.service.InventoryMovementService;
import com.delosreyes.inventario.modules.products.entity.Product;
import com.delosreyes.inventario.modules.products.service.ProductService;
import com.delosreyes.inventario.modules.sales.dto.SaleDtos;
import com.delosreyes.inventario.modules.sales.entity.Sale;
import com.delosreyes.inventario.modules.sales.entity.SaleItem;
import com.delosreyes.inventario.modules.sales.repository.SaleRepository;
import com.delosreyes.inventario.modules.users.entity.User;
import com.delosreyes.inventario.shared.constants.AppConstants;
import com.delosreyes.inventario.shared.exceptions.BusinessException;
import com.delosreyes.inventario.shared.exceptions.ResourceNotFoundException;
import com.delosreyes.inventario.shared.responses.PagedResponse;
import com.delosreyes.inventario.shared.utils.IgvCalculator;
import com.delosreyes.inventario.shared.utils.SaleNumberGenerator;

/**
 * SaleService — Núcleo del proceso de ventas.
 *
 * QUÉ HACE:
 *   Gestiona el ciclo completo de una venta:
 *   1. Validar que todos los productos existen y están activos
 *   2. Verificar stock suficiente para cada item
 *   3. Calcular subtotales, descuento global, IGV (18%) y total
 *   4. Generar número único de venta
 *   5. Persistir Sale + SaleItems en una sola transacción
 *   6. Registrar movimiento de inventario SALE por cada producto
 *   7. Registrar auditoría
 *
 * GARANTÍAS TRANSACCIONALES (@Transactional):
 *   Si falla cualquier paso (ej: stock insuficiente en item 3 de 5),
 *   ningún cambio se persiste: ni la venta, ni el stock, ni los movimientos.
 *   Esto evita ventas parcialmente registradas.
 *
 * CÁLCULO DE TOTALES:
 *   Los precios de venta ya incluyen IGV (ver IgvCalculator).
 *   subtotal    = SUM(quantity * unitPrice - itemDiscount)  por cada item
 *   discount    = descuento global, se resta del precio con IGV
 *   total       = subtotal - discount
 *   tax (IGV)   = parte del total que es IGV: total - total / 1.18
 *
 * RELACIONES:
 *   SaleController → SaleService
 *   SaleService → ProductService (valida y obtiene precios actuales)
 *   SaleService → CustomerService (opcional, asocia cliente)
 *   SaleService → InventoryMovementService (descuenta stock)
 *   SaleService → AuditService (trazabilidad)
 *   SaleService → SaleNumberGenerator (genera VTA-YYYYMMDD-XXXXX)
 */
@Service
@Transactional
public class SaleService {

    private static final Logger log = LoggerFactory.getLogger(SaleService.class);

    private final SaleRepository saleRepository;
    private final ProductService productService;
    private final CustomerService customerService;
    private final InventoryMovementService movementService;
    private final AuditService auditService;
    private final SaleNumberGenerator saleNumberGenerator;

    public SaleService(SaleRepository saleRepository,
                       ProductService productService,
                       CustomerService customerService,
                       InventoryMovementService movementService,
                       AuditService auditService,
                       SaleNumberGenerator saleNumberGenerator) {
        this.saleRepository = saleRepository;
        this.productService = productService;
        this.customerService = customerService;
        this.movementService = movementService;
        this.auditService = auditService;
        this.saleNumberGenerator = saleNumberGenerator;
    }

    // ============================================================
    // CREAR VENTA
    // ============================================================
    public SaleDtos.SaleResponse create(SaleDtos.CreateRequest request, User currentUser) {

        // 1. Construir la entidad Sale
        Sale sale = new Sale();
        sale.setSaleNumber(saleNumberGenerator.generate());
        sale.setUser(currentUser);
        sale.setPaymentMethod(request.getPaymentMethod());
        sale.setNotes(request.getNotes());
        sale.setSaleDate(LocalDateTime.now());
        sale.setStatus(Sale.Status.COMPLETED);

        // 2. Asociar cliente si se proporcionó
        if (request.getCustomerId() != null) {
            Customer customer = customerService.findEntityById(request.getCustomerId());
            sale.setCustomer(customer);
        }

        // 3. Bloquear los productos antes de leer su stock (evita la sobreventa
        //    cuando dos ventas compran el mismo producto al mismo tiempo)
        Map<Long, Product> products = lockProducts(
            request.getItems().stream().map(SaleDtos.ItemRequest::getProductId).toList());

        // 4. Procesar items y validar stock
        BigDecimal subtotal = BigDecimal.ZERO;
        List<SaleItem> saleItems = new ArrayList<>();

        for (SaleDtos.ItemRequest itemRequest : request.getItems()) {
            Product product = products.get(itemRequest.getProductId());

            // Validar que el producto está activo para venta
            productService.validateForSale(product);

            // Validar stock suficiente antes de descontarlo
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new BusinessException("INSUFFICIENT_STOCK",
                    "Stock insuficiente para el producto [" + product.getCode() + "] '"
                    + product.getName() + "'. "
                    + "Disponible: " + product.getStock()
                    + " | Solicitado: " + itemRequest.getQuantity());
            }

            // Calcular subtotal del item
            BigDecimal itemDiscount = itemRequest.getDiscount() != null
                ? itemRequest.getDiscount()
                : BigDecimal.ZERO;

            BigDecimal lineTotal = product.getSalePrice()
                .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            // El descuento puede llegar al 100 % de la línea, pero no superarlo:
            // si no, el subtotal del item queda negativo
            if (itemDiscount.compareTo(lineTotal) > 0) {
                throw new BusinessException("INVALID_DISCOUNT",
                    "El descuento de [" + product.getCode() + "] (S/ " + itemDiscount
                    + ") no puede ser mayor que el importe del producto (S/ "
                    + lineTotal.setScale(2, RoundingMode.HALF_UP) + ")");
            }

            BigDecimal itemSubtotal = lineTotal
                .subtract(itemDiscount)
                .setScale(2, RoundingMode.HALF_UP);

            SaleItem saleItem = new SaleItem();
            saleItem.setProduct(product);
            saleItem.setQuantity(itemRequest.getQuantity());
            saleItem.setUnitPrice(product.getSalePrice());
            saleItem.setDiscount(itemDiscount);
            saleItem.setSubtotal(itemSubtotal);

            saleItems.add(saleItem);
            subtotal = subtotal.add(itemSubtotal);
        }

        // 5. Calcular totales de la venta
        BigDecimal globalDiscount = request.getGlobalDiscount() != null
            ? request.getGlobalDiscount()
            : BigDecimal.ZERO;

        if (globalDiscount.compareTo(subtotal) > 0) {
            throw new BusinessException("INVALID_DISCOUNT",
                "El descuento global (S/ " + globalDiscount
                + ") no puede ser mayor que el subtotal de la venta (S/ " + subtotal + ")");
        }

        // Los precios ya incluyen IGV: el total es subtotal - descuento y el IGV
        // se desglosa de ese total (no se suma encima)
        BigDecimal total = subtotal.subtract(globalDiscount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = IgvCalculator.includedIgv(total);

        sale.setSubtotal(subtotal);
        sale.setDiscount(globalDiscount);
        sale.setTax(tax);
        sale.setTotal(total);

        // 6. Añadir items a la venta
        for (SaleItem item : saleItems) {
            sale.addItem(item);
        }

        // 7. Persistir la venta completa (CASCADE guarda los SaleItems)
        Sale savedSale = saleRepository.save(sale);

        // 8. Registrar movimiento de inventario SALE por cada item
        //    (DESPUÉS de persistir la venta para tener el saleId)
        for (SaleItem item : savedSale.getItems()) {
            movementService.registerSaleMovement(
                item.getProduct(),
                item.getQuantity(),
                item.getProduct().getCostPrice(),
                savedSale.getId(),
                currentUser
            );
        }

        // 9. Auditoría
        auditService.log(AppConstants.MODULE_SALES, AppConstants.ACTION_CREATE,
            currentUser.getId(), currentUser.getEmail(),
            String.valueOf(savedSale.getId()),
            "Venta registrada: " + savedSale.getSaleNumber()
            + " | Total: S/ " + savedSale.getTotal()
            + " | Items: " + savedSale.getItems().size()
            + " | Método: " + savedSale.getPaymentMethod());

        log.info("Venta creada: {} | S/ {} por {}",
            savedSale.getSaleNumber(), savedSale.getTotal(), currentUser.getEmail());

        return toResponse(savedSale);
    }
// ============================================================
    // EDITAR VENTA (campos seguros: cliente, pago, notas)
    // ============================================================
    public SaleDtos.SaleResponse update(Long saleId, SaleDtos.UpdateRequest request, User currentUser) {
        Sale sale = findEntityById(saleId);

        if (sale.getStatus() == Sale.Status.CANCELLED || sale.getStatus() == Sale.Status.REFUNDED) {
            throw new BusinessException("SALE_NOT_EDITABLE",
                "No se puede editar una venta " + sale.getStatus().name().toLowerCase());
        }

        StringBuilder changes = new StringBuilder();

        if (request.getCustomerId() != null) {
            Customer customer = customerService.findEntityById(request.getCustomerId());
            sale.setCustomer(customer);
            changes.append("cliente → ").append(customer.getName()).append(" | ");
        }

        if (request.getPaymentMethod() != null && request.getPaymentMethod() != sale.getPaymentMethod()) {
            changes.append("pago ").append(sale.getPaymentMethod()).append(" → ").append(request.getPaymentMethod()).append(" | ");
            sale.setPaymentMethod(request.getPaymentMethod());
        }

        if (request.getNotes() != null) {
            sale.setNotes(request.getNotes());
            changes.append("notas actualizadas");
        }

        Sale saved = saleRepository.save(sale);

        auditService.log(AppConstants.MODULE_SALES, "UPDATE",
            currentUser.getId(), currentUser.getEmail(),
            String.valueOf(saleId),
            "Venta editada: " + sale.getSaleNumber() + " | " + changes);

        return toResponse(saved);
    }
    // ============================================================
    // CANCELAR VENTA (solo ADMIN)
    // ============================================================
    public SaleDtos.SaleResponse cancel(Long saleId, String reason, User currentUser) {
        // Se bloquea la venta: si dos personas la cancelan a la vez, la segunda
        // espera, ve el estado CANCELLED y no devuelve el stock otra vez.
        Sale sale = saleRepository.findByIdForUpdate(saleId)
            .orElseThrow(() -> new ResourceNotFoundException("Venta", "id", saleId));

        if (sale.getStatus() == Sale.Status.CANCELLED) {
            throw new BusinessException("ALREADY_CANCELLED", "La venta ya está cancelada");
        }
        if (sale.getStatus() == Sale.Status.REFUNDED) {
            throw new BusinessException("ALREADY_REFUNDED", "La venta ya fue reembolsada");
        }

        sale.setStatus(Sale.Status.CANCELLED);
        sale.setNotes((sale.getNotes() != null ? sale.getNotes() + " | " : "") + "CANCELADO: " + reason);

        // Bloquear los productos antes de devolver el stock, igual que al vender
        Map<Long, Product> products = lockProducts(
            sale.getItems().stream().map(item -> item.getProduct().getId()).toList());

        // Revertir stock por cada item: se registra una ENTRY con cantidad
        // positiva y registerSaleReversal se encarga de sumar el stock.
        for (SaleItem item : sale.getItems()) {
            Product product = products.get(item.getProduct().getId());
            movementService.registerSaleReversal(
                product,
                item.getQuantity(),
                product.getCostPrice(),
                sale.getId(),
                currentUser
            );
        }

        Sale saved = saleRepository.save(sale);

        auditService.log(AppConstants.MODULE_SALES, "CANCEL",
            currentUser.getId(), currentUser.getEmail(),
            String.valueOf(saleId),
            "Venta cancelada: " + sale.getSaleNumber() + " | Razón: " + reason);

        return toResponse(saved);
    }

    // ============================================================
    // CONSULTAS
    // ============================================================
    @Transactional(readOnly = true)
    public SaleDtos.SaleResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public SaleDtos.SaleResponse findBySaleNumber(String saleNumber) {
        Sale sale = saleRepository.findBySaleNumber(saleNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Venta", "número", saleNumber));
        return toResponse(sale);
    }

    @Transactional(readOnly = true)
    public PagedResponse<SaleDtos.SaleResponse> findAll(
            Sale.Status status, Long customerId, Long userId,
            LocalDateTime from, LocalDateTime to,
            int page, int size) {

        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size, Sort.by("saleDate").descending());

        Page<Sale> result;
        if (status != null) {
            result = saleRepository.findByStatusOrderBySaleDateDesc(status, pageable);
        } else if (customerId != null) {
            result = saleRepository.findByCustomerIdOrderBySaleDateDesc(customerId, pageable);
        } else if (userId != null) {
            result = saleRepository.findByUserIdOrderBySaleDateDesc(userId, pageable);
        } else if (from != null && to != null) {
            result = saleRepository.findByDateRange(from, to, pageable);
        } else {
            result = saleRepository.findAll(pageable);
        }

        return PagedResponse.from(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public List<SaleDtos.SaleResponse> findRecent(int limit) {
        return saleRepository.findRecentSales(PageRequest.of(0, limit))
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ============================================================
    // Helpers
    // ============================================================
    public Sale findEntityById(Long id) {
        return saleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Venta", "id", id));
    }

    // Bloquea cada producto (SELECT ... FOR UPDATE) siempre en orden de id.
    // Si dos ventas bloquearan los mismos productos en distinto orden, cada una
    // podría quedarse esperando a la otra (deadlock). Con el mismo orden, la
    // segunda simplemente espera a que termine la primera.
    private Map<Long, Product> lockProducts(List<Long> productIds) {
        Map<Long, Product> products = new HashMap<>();
        productIds.stream().distinct().sorted().forEach(id ->
            products.put(id, productService.findEntityByIdForUpdate(id)));
        return products;
    }

    private SaleDtos.SaleResponse toResponse(Sale sale) {
        SaleDtos.SaleResponse r = new SaleDtos.SaleResponse();
        r.setId(sale.getId());
        r.setSaleNumber(sale.getSaleNumber());
        r.setUserId(sale.getUser().getId());
        r.setUserName(sale.getUser().getName());
        r.setStatus(sale.getStatus());
        r.setSubtotal(sale.getSubtotal());
        r.setDiscount(sale.getDiscount());
        r.setTax(sale.getTax());
        r.setTotal(sale.getTotal());
        r.setPaymentMethod(sale.getPaymentMethod());
        r.setNotes(sale.getNotes());
        r.setSaleDate(sale.getSaleDate());
        r.setCreatedAt(sale.getCreatedAt());

        if (sale.getCustomer() != null) {
            r.setCustomerId(sale.getCustomer().getId());
            r.setCustomerName(sale.getCustomer().getName());
        }

        List<SaleDtos.ItemResponse> items = sale.getItems().stream().map(item -> {
            SaleDtos.ItemResponse ir = new SaleDtos.ItemResponse();
            ir.setId(item.getId());
            ir.setProductId(item.getProduct().getId());
            ir.setProductCode(item.getProduct().getCode());
            ir.setProductName(item.getProduct().getName());
            ir.setQuantity(item.getQuantity());
            ir.setUnitPrice(item.getUnitPrice());
            ir.setDiscount(item.getDiscount());
            ir.setSubtotal(item.getSubtotal());
            return ir;
        }).collect(Collectors.toList());

        r.setItems(items);
        return r;
    }
}