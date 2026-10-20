package com.delosreyes.inventario.modules.movements.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delosreyes.inventario.modules.audit.service.AuditService;
import com.delosreyes.inventario.modules.movements.dto.MovementDtos;
import com.delosreyes.inventario.modules.movements.entity.InventoryMovement;
import com.delosreyes.inventario.modules.movements.repository.InventoryMovementRepository;
import com.delosreyes.inventario.modules.products.entity.Product;
import com.delosreyes.inventario.modules.products.repository.ProductRepository;
import com.delosreyes.inventario.modules.products.service.ProductService;
import com.delosreyes.inventario.modules.users.entity.User;
import com.delosreyes.inventario.shared.constants.AppConstants;
import com.delosreyes.inventario.shared.exceptions.BusinessException;
import com.delosreyes.inventario.shared.responses.PagedResponse;

/**
 * InventoryMovementService — Núcleo del sistema de inventario.
 *
 * QUÉ HACE:
 *   Gestiona todos los movimientos de stock con garantías transaccionales.
 *
 * FLUJO:
 *   1. Valida movimiento
 *   2. Actualiza stock
 *   3. Registra historial
 *   4. Registra auditoría
 */
@Service
@Transactional
public class InventoryMovementService {

    private static final Logger log = LoggerFactory.getLogger(InventoryMovementService.class);

    private final InventoryMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final AuditService auditService;

    public InventoryMovementService(InventoryMovementRepository movementRepository,
                                     ProductRepository productRepository,
                                     ProductService productService,
                                     AuditService auditService) {
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.auditService = auditService;
    }


    // ============================================================
    // REGISTRAR MOVIMIENTO MANUAL (ENTRY / EXIT)
    // ============================================================
    // Todo movimiento que cambia el stock invalida la caché "dashboard":
    // getSummary() la guarda sin vencimiento, y summary.lowStockCount es lo
    // que enciende la campanita de stock bajo en el frontend.
    @CacheEvict(value = AppConstants.CACHE_DASHBOARD, allEntries = true)
    public MovementDtos.MovementResponse register(
            MovementDtos.CreateRequest request, User currentUser) {

        if (request.getType() == InventoryMovement.Type.SALE) {
            throw new BusinessException("INVALID_TYPE",
                "Las ventas se registran a través del módulo de ventas");
        }

        if (request.getType() == InventoryMovement.Type.ADJUSTMENT) {
            throw new BusinessException("INVALID_TYPE",
                "Los ajustes se registran a través del endpoint de ajuste de inventario");
        }

        Product product = productService.findEntityByIdForUpdate(request.getProductId());

        return processMovement(
            product,
            request.getType(),
            request.getQuantity(),
            request.getUnitCost(),
            request.getObservation(),
            null,
            currentUser
        );
    }


    // ============================================================
    // AJUSTE DE INVENTARIO (solo ADMIN)
    // ============================================================
    @CacheEvict(value = AppConstants.CACHE_DASHBOARD, allEntries = true)
    public MovementDtos.MovementResponse registerAdjustment(
            Long productId,
            Integer newStock,
            String observation,
            User currentUser) {

        Product product = productService.findEntityByIdForUpdate(productId);

        int difference = newStock - product.getStock();

        if (difference == 0) {
            throw new BusinessException("NO_CHANGE",
                "El stock ya es " + newStock);
        }

        int stockBefore = product.getStock();

        product.setStock(newStock);
        productRepository.save(product);


        InventoryMovement movement = buildMovement(
            product,
            currentUser,
            InventoryMovement.Type.ADJUSTMENT,
            Math.abs(difference),
            stockBefore,
            newStock,
            null,
            observation,
            null
        );


        InventoryMovement saved = movementRepository.save(movement);


        auditService.log(
            AppConstants.MODULE_INVENTORY,
            "ADJUSTMENT",
            currentUser.getId(),
            currentUser.getEmail(),
            String.valueOf(productId),
            "Ajuste de stock: " + product.getCode()
            + " | " + stockBefore
            + " → " + newStock
        );


        return toResponse(saved);
    }


    // ============================================================
    // MOVIMIENTO DE VENTA
    // ============================================================
    @CacheEvict(value = AppConstants.CACHE_DASHBOARD, allEntries = true)
    public void registerSaleMovement(
            Product product,
            int quantity,
            BigDecimal unitCost,
            Long saleId,
            User currentUser) {

        processMovement(
            product,
            InventoryMovement.Type.SALE,
            quantity,
            unitCost,
            "Venta #" + saleId,
            saleId,
            currentUser
        );
    }


    // ============================================================
    // REVERSIÓN DE VENTA (llamado desde SaleService.cancel())
    // ============================================================
    // Cancelar una venta devuelve stock, así que se registra como ENTRY con
    // cantidad positiva (la BD exige quantity > 0). processMovement actualiza
    // el stock, guarda el historial y registra la auditoría.
    @CacheEvict(value = AppConstants.CACHE_DASHBOARD, allEntries = true)
    public void registerSaleReversal(
            Product product,
            int quantity,
            BigDecimal unitCost,
            Long saleId,
            User currentUser) {

        processMovement(
            product,
            InventoryMovement.Type.ENTRY,
            quantity,
            unitCost,
            "Reversión por cancelación de venta #" + saleId,
            saleId,
            currentUser
        );
    }


    // ============================================================
    // CONSULTAS
    // ============================================================
    @Transactional(readOnly = true)
    public PagedResponse<MovementDtos.MovementResponse> findByProduct(
            Long productId,
            int page,
            int size) {

        Pageable pageable =
            PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE));

        return PagedResponse.from(
            movementRepository
                .findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(this::toResponse)
        );
    }


    @Transactional(readOnly = true)
    public PagedResponse<MovementDtos.MovementResponse> findAll(
            InventoryMovement.Type type,
            int page,
            int size) {

        Pageable pageable =
            PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE));


        Page<InventoryMovement> result =
            type != null
            ? movementRepository.findByTypeOrderByCreatedAtDesc(type, pageable)
            : movementRepository.findAll(pageable);


        return PagedResponse.from(
            result.map(this::toResponse)
        );
    }


    @Transactional(readOnly = true)
    public PagedResponse<MovementDtos.MovementResponse> findByDateRange(
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size) {

        Pageable pageable =
            PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE));


        return PagedResponse.from(
            movementRepository
                .findByDateRange(from, to, pageable)
                .map(this::toResponse)
        );
    }


    @Transactional(readOnly = true)
    public List<MovementDtos.MovementResponse> findRecent(int limit) {

        return movementRepository
            .findRecent(PageRequest.of(0, limit))
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }



    // ============================================================
    // PRIVADOS — LÓGICA COMPARTIDA
    // ============================================================
    private MovementDtos.MovementResponse processMovement(
            Product product,
            InventoryMovement.Type type,
            int quantity,
            BigDecimal unitCost,
            String observation,
            Long saleId,
            User currentUser) {


        int stockBefore = product.getStock();
        int stockAfter;


        switch(type) {

            case ENTRY ->
                stockAfter = stockBefore + quantity;


            case EXIT, SALE -> {

                if(stockBefore < quantity) {

                    throw new BusinessException(
                        "INSUFFICIENT_STOCK",
                        "Stock insuficiente para [" +
                        product.getCode() +
                        "]. Disponible: " +
                        stockBefore +
                        " | Solicitado: " +
                        quantity
                    );
                }

                stockAfter = stockBefore - quantity;
            }


            default ->
                throw new BusinessException(
                    "INVALID_TYPE",
                    "Tipo inválido: " + type
                );
        }



        product.setStock(stockAfter);
        productRepository.save(product);



        InventoryMovement movement =
            buildMovement(
                product,
                currentUser,
                type,
                quantity,
                stockBefore,
                stockAfter,
                unitCost,
                observation,
                saleId
            );


        InventoryMovement saved =
            movementRepository.save(movement);



        auditService.log(
            AppConstants.MODULE_INVENTORY,
            type.name(),
            currentUser.getId(),
            currentUser.getEmail(),
            String.valueOf(product.getId()),
            type +
            " | " +
            product.getCode() +
            " | qty=" +
            quantity +
            " | stock: " +
            stockBefore +
            "→" +
            stockAfter
        );


        log.info(
            "Movimiento [{}]: {} qty={} stock:{}→{}",
            type,
            product.getCode(),
            quantity,
            stockBefore,
            stockAfter
        );


        return toResponse(saved);
    }



    private InventoryMovement buildMovement(
            Product product,
            User user,
            InventoryMovement.Type type,
            int quantity,
            int stockBefore,
            int stockAfter,
            BigDecimal unitCost,
            String observation,
            Long saleId) {


        InventoryMovement m = new InventoryMovement();

        m.setProduct(product);
        m.setUser(user);
        m.setType(type);
        m.setQuantity(quantity);
        m.setStockBefore(stockBefore);
        m.setStockAfter(stockAfter);
        m.setUnitCost(unitCost);
        m.setObservation(observation);
        m.setSaleId(saleId);

        return m;
    }



    private MovementDtos.MovementResponse toResponse(
            InventoryMovement m) {

        MovementDtos.MovementResponse r =
            new MovementDtos.MovementResponse();

        r.setId(m.getId());
        r.setProductId(m.getProduct().getId());
        r.setProductCode(m.getProduct().getCode());
        r.setProductName(m.getProduct().getName());

        r.setUserId(m.getUser().getId());
        r.setUserName(m.getUser().getName());

        r.setSaleId(m.getSaleId());

        r.setType(m.getType());
        r.setQuantity(m.getQuantity());

        r.setStockBefore(m.getStockBefore());
        r.setStockAfter(m.getStockAfter());

        r.setUnitCost(m.getUnitCost());
        r.setObservation(m.getObservation());

        r.setCreatedAt(m.getCreatedAt());

        return r;
    }
}