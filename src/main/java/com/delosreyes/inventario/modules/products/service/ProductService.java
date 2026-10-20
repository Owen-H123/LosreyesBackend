package com.delosreyes.inventario.modules.products.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delosreyes.inventario.modules.audit.service.AuditService;
import com.delosreyes.inventario.modules.categories.entity.Category;
import com.delosreyes.inventario.modules.categories.service.CategoryService;
import com.delosreyes.inventario.modules.products.dto.ProductDtos;
import com.delosreyes.inventario.modules.products.entity.Product;
import com.delosreyes.inventario.modules.products.mapper.ProductMapper;
import com.delosreyes.inventario.modules.products.repository.ProductRepository;
import com.delosreyes.inventario.modules.users.entity.User;
import com.delosreyes.inventario.modules.vehicles.entity.Vehicle;
import com.delosreyes.inventario.modules.vehicles.service.VehicleService;
import com.delosreyes.inventario.shared.constants.AppConstants;
import com.delosreyes.inventario.shared.exceptions.BusinessException;
import com.delosreyes.inventario.shared.exceptions.DuplicateResourceException;
import com.delosreyes.inventario.shared.exceptions.ResourceNotFoundException;
import com.delosreyes.inventario.shared.responses.PagedResponse;
import com.delosreyes.inventario.shared.utils.IgvCalculator;

/**
 * ProductService — Núcleo de negocio del módulo de productos.
 *
 * QUÉ HACE:
 *   CRUD completo de productos con validaciones de negocio.
 *   Gestión de compatibilidad con vehículos (many-to-many).
 *   Soft delete: al eliminar, marca deleted=true sin borrar de BD.
 *   Búsqueda paginada con filtros múltiples.
 *   Cálculo de margen de ganancia en cada respuesta.
 *
 * REGLAS DE NEGOCIO:
 *   - El código de producto debe ser único en todo el sistema.
 *   - El precio de venta debe ser mayor al precio de costo.
 *   - El stock inicial se registra como movimiento ENTRY para trazabilidad.
 *   - No se puede eliminar un producto con ventas históricas (soft delete únicamente).
 *   - Un producto INACTIVE o DISCONTINUED no puede ser vendido.
 *
 * RELACIONES:
 *   ProductController → ProductService → ProductRepository
 *   ProductService → CategoryService (valida categoría)
 *   ProductService → VehicleService (valida vehículos)
 *   ProductService → AuditService (trazabilidad)
 *
 * NOTA SOBRE STOCK:
 *   El stock NO se modifica directamente aquí al crear un producto.
 *   El inventario inicial se registra a través de InventoryMovementService
 *   para que quede en el historial de movimientos.
 *   EXCEPCIÓN: La creación inicial establece el stock directamente
 *   y el movimiento ENTRY se genera en InventoryMovementService.
 */
@Service
@Transactional
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryService categoryService;
    private final VehicleService vehicleService;
    private final AuditService auditService;

    public ProductService(ProductRepository productRepository,
                          ProductMapper productMapper,
                          CategoryService categoryService,
                          VehicleService vehicleService,
                          AuditService auditService) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.categoryService = categoryService;
        this.vehicleService = vehicleService;
        this.auditService = auditService;
    }

    // ============================================================
    // CREAR PRODUCTO
    // ============================================================
    // También invalida la caché "dashboard": el stock inicial y el mínimo
    // entran en summary.lowStockCount, que enciende la campanita de stock bajo.
    @Caching(evict = {
        @CacheEvict(value = AppConstants.CACHE_PRODUCTS,  allEntries = true),
        @CacheEvict(value = AppConstants.CACHE_DASHBOARD, allEntries = true),
    })
    public ProductDtos.ProductResponse create(ProductDtos.CreateRequest request, User currentUser) {
        // Validar unicidad del código
        if (productRepository.existsByCodeAndDeletedFalse(request.getCode())) {
            throw new DuplicateResourceException("Producto", "código", request.getCode());
        }

        // Validar precio de venta > precio de costo
        validatePrices(request.getCostPrice(), request.getSalePrice());

        // Construir entidad
        Product product = productMapper.toEntity(request);

        // Asignar categoría validada
        Category category = categoryService.findEntityById(request.getCategoryId());
        product.setCategory(category);

        // Asignar vehículos compatibles si se enviaron
        if (request.getVehicleIds() != null && !request.getVehicleIds().isEmpty()) {
            Set<Vehicle> vehicles = resolveVehicles(request.getVehicleIds());
            product.setVehicles(vehicles);
        }

        Product saved = productRepository.save(product);

        auditService.log(AppConstants.MODULE_PRODUCTS, AppConstants.ACTION_CREATE,
            currentUser.getId(), currentUser.getEmail(),
            String.valueOf(saved.getId()),
            "Producto creado: [" + saved.getCode() + "] " + saved.getName() +
            " - Stock inicial: " + saved.getStock());

        log.info("Producto creado: {} [{}] por {}", saved.getCode(), saved.getName(), currentUser.getEmail());
        return enrichResponse(productMapper.toResponse(saved));
    }

    // ============================================================
    // ACTUALIZAR PRODUCTO
    // ============================================================
    // Cambiar minimumStock (o el status del producto) también cambia
    // quién cuenta como "stock bajo" — misma razón que en create().
    @Caching(evict = {
        @CacheEvict(value = AppConstants.CACHE_PRODUCTS,  allEntries = true),
        @CacheEvict(value = AppConstants.CACHE_DASHBOARD, allEntries = true),
    })
    public ProductDtos.ProductResponse update(Long id, ProductDtos.UpdateRequest request, User currentUser) {
        Product product = findEntityById(id);

        validatePrices(request.getCostPrice(), request.getSalePrice());

        Category category = categoryService.findEntityById(request.getCategoryId());
        productMapper.updateEntity(request, product);
        product.setCategory(category);

        // Actualizar vehículos compatibles
        if (request.getVehicleIds() != null) {
            product.setVehicles(resolveVehicles(request.getVehicleIds()));
        }

        Product saved = productRepository.save(product);

        auditService.log(AppConstants.MODULE_PRODUCTS, AppConstants.ACTION_UPDATE,
            currentUser.getId(), currentUser.getEmail(),
            String.valueOf(id),
            "Producto actualizado: [" + saved.getCode() + "] " + saved.getName());

        return enrichResponse(productMapper.toResponse(saved));
    }

    // ============================================================
    // ELIMINAR (soft delete)
    // ============================================================
    // Un producto eliminado deja de contar en activeProductsCount y
    // lowStockCount — misma razón que en create()/update().
    @Caching(evict = {
        @CacheEvict(value = AppConstants.CACHE_PRODUCTS,  allEntries = true),
        @CacheEvict(value = AppConstants.CACHE_DASHBOARD, allEntries = true),
    })
    public void delete(Long id, User currentUser) {
        Product product = findEntityById(id);
        product.setDeleted(true);
        product.setStatus(Product.Status.INACTIVE);
        productRepository.save(product);

        auditService.log(AppConstants.MODULE_PRODUCTS, AppConstants.ACTION_DELETE,
            currentUser.getId(), currentUser.getEmail(),
            String.valueOf(id),
            "Producto eliminado (soft): [" + product.getCode() + "] " + product.getName());

        log.info("Producto eliminado (soft): {} por {}", product.getCode(), currentUser.getEmail());
    }

    // ============================================================
    // CONSULTAS
    // ============================================================
    @Transactional(readOnly = true)
    public ProductDtos.ProductResponse findById(Long id) {
        return enrichResponse(productMapper.toResponse(findEntityById(id)));
    }

    @Transactional(readOnly = true)
    public ProductDtos.ProductResponse findByCode(String code) {
        Product product = productRepository.findByCodeAndDeletedFalse(code)
            .orElseThrow(() -> new ResourceNotFoundException("Producto", "código", code));
        return enrichResponse(productMapper.toResponse(product));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductDtos.ProductResponse> findAll(
            String search, Long categoryId, Product.Status status,
            String sortBy, String sortDir, int page, int size) {

        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Sort sort = "asc".equalsIgnoreCase(sortDir)
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> result;

        if (search != null && !search.isBlank()) {
            result = productRepository.search(search, pageable);
        } else if (categoryId != null) {
            result = productRepository.findByCategoryIdAndDeletedFalse(categoryId, pageable);
        } else if (status != null) {
            result = productRepository.findByStatusAndDeletedFalse(status, pageable);
        } else {
            result = productRepository.findAll(
                (root, q, cb) -> cb.equal(root.get("deleted"), false), pageable);
        }

        return PagedResponse.from(result.map(p -> enrichResponse(productMapper.toResponse(p))));
    }

    @Transactional(readOnly = true)
    public List<ProductDtos.ProductResponse> findLowStock() {
        return productRepository.findLowStock()
            .stream()
            .map(p -> enrichResponse(productMapper.toResponse(p)))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductDtos.ProductResponse> findByVehicle(Long vehicleId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, AppConstants.MAX_PAGE_SIZE));
        return PagedResponse.from(
            productRepository.findByVehicleId(vehicleId, pageable)
                .map(p -> enrichResponse(productMapper.toResponse(p)))
        );
    }

    // ============================================================
    // Acceso interno para otros módulos
    // ============================================================
    public Product findEntityById(Long id) {
        return productRepository.findById(id)
            .filter(p -> !p.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", id));
    }

    // Igual que findEntityById, pero bloquea la fila del producto (SELECT ... FOR UPDATE)
    // hasta que termina la transacción. Usar en todo flujo que cambie el stock: otra
    // transacción que quiera el mismo producto espera y luego ve el stock actualizado.
    // Debe ser la primera carga del producto en la transacción.
    public Product findEntityByIdForUpdate(Long id) {
        return productRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", id));
    }

    public void validateForSale(Product product) {
        if (product.getStatus() != Product.Status.ACTIVE) {
            throw new BusinessException("PRODUCT_INACTIVE",
                "El producto [" + product.getCode() + "] no está disponible para venta");
        }
    }

    // ============================================================
    // Conteos para Dashboard
    // ============================================================
    @Transactional(readOnly = true)
    public long countLowStock() {
        return productRepository.countLowStock();
    }

    @Transactional(readOnly = true)
    public long countActive() {
        return productRepository.countActive();
    }

    // ============================================================
    // Helpers privados
    // ============================================================
    private Set<Vehicle> resolveVehicles(Set<Long> vehicleIds) {
        Set<Vehicle> vehicles = new HashSet<>();
        for (Long vehicleId : vehicleIds) {
            vehicles.add(vehicleService.findEntityById(vehicleId));
        }
        return vehicles;
    }

    // salePrice incluye IGV y costPrice no: se comparan los dos sin IGV.
    // Si no, un producto podría venderse a pérdida y la validación lo aceptaría.
    private void validatePrices(BigDecimal costPrice, BigDecimal salePrice) {
        if (IgvCalculator.withoutIgv(salePrice).compareTo(costPrice) <= 0) {
            throw new BusinessException("INVALID_PRICE",
                "El precio de venta sin IGV debe ser mayor al precio de costo");
        }
    }

    /**
     * Calcula y agrega campos derivados que no pueden venir del mapper:
     *   profitMargin = ((salePrice sin IGV - costPrice) / costPrice) * 100
     * El IGV no es ganancia (se paga a SUNAT), por eso se excluye del margen.
     */
    private ProductDtos.ProductResponse enrichResponse(ProductDtos.ProductResponse response) {
        if (response.getCostPrice() != null && response.getSalePrice() != null
                && response.getCostPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal margin = IgvCalculator.withoutIgv(response.getSalePrice())
                .subtract(response.getCostPrice())
                .divide(response.getCostPrice(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
            response.setProfitMargin(margin);
        }
        return response;
    }
}