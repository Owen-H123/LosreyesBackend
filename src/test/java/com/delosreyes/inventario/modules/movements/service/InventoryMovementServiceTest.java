package com.delosreyes.inventario.modules.movements.service;

import com.delosreyes.inventario.modules.audit.service.AuditService;
import com.delosreyes.inventario.modules.movements.dto.MovementDtos;
import com.delosreyes.inventario.modules.movements.entity.InventoryMovement;
import com.delosreyes.inventario.modules.movements.repository.InventoryMovementRepository;
import com.delosreyes.inventario.modules.products.entity.Product;
import com.delosreyes.inventario.modules.products.repository.ProductRepository;
import com.delosreyes.inventario.modules.products.service.ProductService;
import com.delosreyes.inventario.modules.users.entity.User;
import com.delosreyes.inventario.shared.exceptions.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de las entradas, salidas y ajustes manuales de inventario.
 * Producto de prueba con stock 10.
 */
class InventoryMovementServiceTest {

    private InventoryMovementRepository movementRepository;
    private InventoryMovementService service;
    private Product product;
    private User admin;

    @BeforeEach
    void setUp() {
        movementRepository = mock(InventoryMovementRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        ProductService productService = mock(ProductService.class);

        service = new InventoryMovementService(movementRepository, productRepository,
            productService, mock(AuditService.class));

        product = new Product();
        product.setId(10L);
        product.setCode("PEU-001");
        product.setName("Pastilla de freno");
        product.setStock(10);

        admin = new User();
        admin.setId(1L);
        admin.setName("Admin");
        admin.setEmail("admin@delosreyes.com");

        when(productService.findEntityByIdForUpdate(10L)).thenReturn(product);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void unaEntradaSumaAlStockYQuedaRegistrada() {
        service.register(movement(InventoryMovement.Type.ENTRY, 5), admin);

        assertThat(product.getStock()).isEqualTo(15);
        InventoryMovement saved = savedMovement();
        assertThat(saved.getType()).isEqualTo(InventoryMovement.Type.ENTRY);
        assertThat(saved.getStockBefore()).isEqualTo(10);
        assertThat(saved.getStockAfter()).isEqualTo(15);
    }

    @Test
    void unaSalidaRestaDelStock() {
        service.register(movement(InventoryMovement.Type.EXIT, 4), admin);

        assertThat(product.getStock()).isEqualTo(6);
        assertThat(savedMovement().getStockAfter()).isEqualTo(6);
    }

    @Test
    void unaSalidaMayorAlStockSeRechazaYNoCambiaNada() {
        assertThatThrownBy(() -> service.register(movement(InventoryMovement.Type.EXIT, 11), admin))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Stock insuficiente");

        assertThat(product.getStock()).isEqualTo(10);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void lasVentasNoSePuedenRegistrarComoMovimientoManual() {
        assertThatThrownBy(() -> service.register(movement(InventoryMovement.Type.SALE, 1), admin))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("módulo de ventas");

        assertThat(product.getStock()).isEqualTo(10);
    }

    @Test
    void unAjusteFijaElStockAlValorIndicado() {
        service.registerAdjustment(10L, 3, "Conteo físico", admin);

        assertThat(product.getStock()).isEqualTo(3);
        InventoryMovement saved = savedMovement();
        assertThat(saved.getType()).isEqualTo(InventoryMovement.Type.ADJUSTMENT);
        assertThat(saved.getQuantity()).isEqualTo(7);   // diferencia absoluta
        assertThat(saved.getStockBefore()).isEqualTo(10);
        assertThat(saved.getStockAfter()).isEqualTo(3);
    }

    @Test
    void unAjusteAlMismoStockSeRechaza() {
        assertThatThrownBy(() -> service.registerAdjustment(10L, 10, "Sin cambios", admin))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ya es 10");
    }

    private MovementDtos.CreateRequest movement(InventoryMovement.Type type, int quantity) {
        MovementDtos.CreateRequest request = new MovementDtos.CreateRequest();
        request.setProductId(10L);
        request.setType(type);
        request.setQuantity(quantity);
        request.setUnitCost(new BigDecimal("50.00"));
        return request;
    }

    private InventoryMovement savedMovement() {
        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(captor.capture());
        return captor.getValue();
    }
}
