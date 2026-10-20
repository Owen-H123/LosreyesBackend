package com.delosreyes.inventario.modules.catalog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delosreyes.inventario.modules.categories.dto.CategoryDtos;
import com.delosreyes.inventario.modules.categories.service.CategoryService;
import com.delosreyes.inventario.modules.products.entity.Product;
import com.delosreyes.inventario.modules.products.repository.ProductRepository;
import com.delosreyes.inventario.shared.exceptions.ResourceNotFoundException;
import com.delosreyes.inventario.shared.responses.ApiResponse;
import com.delosreyes.inventario.shared.responses.PagedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;

/**
 * Catálogo público para la tienda online. Sin login y solo lectura.
 * Solo expone productos ACTIVOS y no eliminados, y nunca el costo, el margen
 * ni el stock mínimo: el stock se publica como cantidad disponible.
 */
@RestController
@RequestMapping("/public/catalog")
@Tag(name = "Catálogo público", description = "Lectura del catálogo para la tienda online")
public class PublicCatalogController {

    private static final int MAX_SIZE = 48;

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public PublicCatalogController(ProductRepository productRepository, CategoryService categoryService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
    }

    public record PublicProduct(
        Long id, String code, String name, String description, String brand,
        Long categoryId, String categoryName, BigDecimal price, Integer stock, String imageUrl, boolean featured) {

        static PublicProduct from(Product p) {
            return new PublicProduct(
                p.getId(), p.getCode(), p.getName(), p.getDescription(), p.getBrand(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getSalePrice(), Math.max(p.getStock(), 0), p.getImageUrl(), p.isWebFeatured());
        }
    }

    private static Specification<Product> visible() {
        return (root, q, cb) -> cb.and(
            cb.isFalse(root.get("deleted")),
            cb.isTrue(root.get("webPublished")),
            cb.equal(root.get("status"), Product.Status.ACTIVE));
    }

    @GetMapping("/products")
    @Transactional(readOnly = true)
    @Operation(summary = "Listar productos visibles en la tienda")
    public ResponseEntity<ApiResponse<PagedResponse<PublicProduct>>> products(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "false") boolean featured,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {

        Specification<Product> spec = visible().and((root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                ps.add(cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("code")), like),
                    cb.like(cb.lower(root.get("brand")), like)));
            }
            if (categoryId != null) ps.add(cb.equal(root.get("category").get("id"), categoryId));
            if (brand != null && !brand.isBlank()) ps.add(cb.equal(root.get("brand"), brand));
            if (featured) ps.add(cb.isTrue(root.get("webFeatured")));
            return cb.and(ps.toArray(new Predicate[0]));
        });

        Sort order = switch (sort) {
            case "price_asc"  -> Sort.by("salePrice").ascending();
            case "price_desc" -> Sort.by("salePrice").descending();
            case "newest"     -> Sort.by("createdAt").descending();
            default           -> Sort.by("name").ascending();
        };

        Page<PublicProduct> result = productRepository
            .findAll(spec, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_SIZE), order))
            .map(PublicProduct::from);

        return ResponseEntity.ok(ApiResponse.success("Catálogo", PagedResponse.from(result)));
    }

    @GetMapping("/products/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "Detalle de un producto visible")
    public ResponseEntity<ApiResponse<PublicProduct>> product(@PathVariable Long id) {
        Product p = productRepository.findOne(visible().and((root, q, cb) -> cb.equal(root.get("id"), id)))
            .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", id));
        return ResponseEntity.ok(ApiResponse.success("Producto", PublicProduct.from(p)));
    }

    @GetMapping("/categories")
    @Operation(summary = "Categorías activas")
    public ResponseEntity<ApiResponse<List<CategoryDtos.Response>>> categories() {
        return ResponseEntity.ok(ApiResponse.success("Categorías", categoryService.findActive()));
    }
}
