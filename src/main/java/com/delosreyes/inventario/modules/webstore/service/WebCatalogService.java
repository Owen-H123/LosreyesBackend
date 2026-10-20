package com.delosreyes.inventario.modules.webstore.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delosreyes.inventario.modules.products.entity.Product;
import com.delosreyes.inventario.modules.products.repository.ProductRepository;
import com.delosreyes.inventario.modules.webstore.dto.WebstoreDtos.CatalogFilter;
import com.delosreyes.inventario.modules.webstore.dto.WebstoreDtos.CatalogItem;
import com.delosreyes.inventario.modules.webstore.dto.WebstoreDtos.CatalogUpdate;
import com.delosreyes.inventario.shared.exceptions.ResourceNotFoundException;
import com.delosreyes.inventario.shared.responses.PagedResponse;

import jakarta.persistence.criteria.Predicate;

/**
 * Qué productos activos se ven en la tienda online y cuáles van destacados.
 * Ocultar un producto aquí no lo desactiva en el ERP.
 */
@Service
public class WebCatalogService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository products;

    public WebCatalogService(ProductRepository products) {
        this.products = products;
    }

    @Transactional(readOnly = true)
    public PagedResponse<CatalogItem> list(CatalogFilter f, int page, int size) {
        Specification<Product> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isFalse(root.get("deleted")));
            ps.add(cb.equal(root.get("status"), Product.Status.ACTIVE));
            if (f.published() != null) ps.add(cb.equal(root.get("webPublished"), f.published()));
            if (f.featured()) ps.add(cb.isTrue(root.get("webFeatured")));
            if (f.search() != null && !f.search().isBlank()) {
                String like = "%" + f.search().trim().toLowerCase() + "%";
                ps.add(cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("code")), like),
                    cb.like(cb.lower(root.get("brand")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by("name"));
        return PagedResponse.from(products.findAll(spec, pageable).map(CatalogItem::from));
    }

    @Transactional
    public CatalogItem update(Long id, CatalogUpdate req) {
        Product p = products.findById(id).filter(x -> !x.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", id));
        if (req.published() != null) p.setWebPublished(req.published());
        if (req.featured() != null) p.setWebFeatured(req.featured());
        // Un producto oculto no puede quedar destacado
        if (!p.isWebPublished()) p.setWebFeatured(false);
        return CatalogItem.from(p);
    }
}
