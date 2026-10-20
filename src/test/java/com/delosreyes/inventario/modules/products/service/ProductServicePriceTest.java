package com.delosreyes.inventario.modules.products.service;

import com.delosreyes.inventario.modules.audit.service.AuditService;
import com.delosreyes.inventario.modules.categories.entity.Category;
import com.delosreyes.inventario.modules.categories.service.CategoryService;
import com.delosreyes.inventario.modules.products.dto.ProductDtos;
import com.delosreyes.inventario.modules.products.entity.Product;
import com.delosreyes.inventario.modules.products.mapper.ProductMapper;
import com.delosreyes.inventario.modules.products.repository.ProductRepository;
import com.delosreyes.inventario.modules.users.entity.User;
import com.delosreyes.inventario.modules.vehicles.service.VehicleService;
import com.delosreyes.inventario.shared.exceptions.BusinessException;
import com.delosreyes.inventario.shared.exceptions.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reglas de precio al crear un producto.
 *
 * El precio de venta se guarda CON IGV y el costo SIN IGV: la validación y el
 * margen deben comparar los dos sin IGV.
 */
class ProductServicePriceTest {

    private ProductRepository productRepository;
    private ProductService productService;
    private User admin;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        CategoryService categoryService = mock(CategoryService.class);

        productService = new ProductService(productRepository, productMapper, categoryService,
            mock(VehicleService.class), mock(AuditService.class));

        admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@delosreyes.com");

        when(categoryService.findEntityById(1L)).thenReturn(new Category());
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Mapper simulado: copia los campos que usan estas pruebas
        when(productMapper.toEntity(any())).thenAnswer(inv -> {
            ProductDtos.CreateRequest r = inv.getArgument(0);
            Product p = new Product();
            p.setCode(r.getCode());
            p.setCostPrice(r.getCostPrice());
            p.setSalePrice(r.getSalePrice());
            p.setStock(r.getStock());
            return p;
        });
        when(productMapper.toResponse(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            ProductDtos.ProductResponse response = new ProductDtos.ProductResponse();
            response.setCostPrice(p.getCostPrice());
            response.setSalePrice(p.getSalePrice());
            return response;
        });
    }

    @Test
    void precioSinIgvMayorAlCostoSeGuardaYCalculaElMargenSinIgv() {
        // Costo 20.00; precio sin IGV 26.00 -> con IGV 30.68. Margen: (26 - 20) / 20 = 30 %
        var response = productService.create(request("20.00", "30.68"), admin);

        assertThat(response.getProfitMargin()).isEqualByComparingTo("30.00");
    }

    @Test
    void precioConIgvMayorAlCostoPeroSinIgvMenorSeRechaza() {
        // Con IGV 22.42 supera al costo 20.00, pero sin IGV es 19.00: se vendería a pérdida
        assertThatThrownBy(() -> productService.create(request("20.00", "22.42"), admin))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("sin IGV debe ser mayor al precio de costo");

        verify(productRepository, never()).save(any());
    }

    @Test
    void precioSinIgvIgualAlCostoSeRechaza() {
        // 23.60 con IGV = 20.00 sin IGV: igual al costo, ganancia cero
        assertThatThrownBy(() -> productService.create(request("20.00", "23.60"), admin))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void codigoRepetidoSeRechaza() {
        when(productRepository.existsByCodeAndDeletedFalse("PEU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request("20.00", "30.68"), admin))
            .isInstanceOf(DuplicateResourceException.class);

        verify(productRepository, never()).save(any());
    }

    private ProductDtos.CreateRequest request(String costPrice, String salePrice) {
        ProductDtos.CreateRequest request = new ProductDtos.CreateRequest();
        request.setCode("PEU-001");
        request.setName("Pastilla de freno");
        request.setCategoryId(1L);
        request.setCostPrice(new BigDecimal(costPrice));
        request.setSalePrice(new BigDecimal(salePrice));
        request.setStock(10);
        return request;
    }
}
