package com.delosreyes.inventario.modules.sales.service;

import com.delosreyes.inventario.modules.audit.service.AuditService;
import com.delosreyes.inventario.modules.customers.service.CustomerService;
import com.delosreyes.inventario.modules.movements.entity.InventoryMovement;
import com.delosreyes.inventario.modules.movements.repository.InventoryMovementRepository;
import com.delosreyes.inventario.modules.movements.service.InventoryMovementService;
import com.delosreyes.inventario.modules.products.entity.Product;
import com.delosreyes.inventario.modules.products.repository.ProductRepository;
import com.delosreyes.inventario.modules.products.service.ProductService;
import com.delosreyes.inventario.modules.sales.entity.Sale;
import com.delosreyes.inventario.modules.sales.entity.SaleItem;
import com.delosreyes.inventario.modules.sales.repository.SaleRepository;
import com.delosreyes.inventario.modules.users.entity.User;
import com.delosreyes.inventario.shared.exceptions.BusinessException;
import com.delosreyes.inventario.shared.utils.SaleNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de SaleService.cancel().
 *
 * Se usa el InventoryMovementService real para comprobar el efecto completo
 * sobre el stock y el movimiento guardado. Solo los repositorios y servicios
 * externos son simulados con Mockito.
 */
class SaleServiceCancelTest {

    private SaleRepository saleRepository;
    private ProductService productService;
    private InventoryMovementRepository movementRepository;
    private SaleService saleService;

    private User admin;
    private Product product;
    private Sale sale;

    @BeforeEach
    void setUp() {
        saleRepository = mock(SaleRepository.class);
        movementRepository = mock(InventoryMovementRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        AuditService auditService = mock(AuditService.class);

        InventoryMovementService movementService = new InventoryMovementService(
            movementRepository, productRepository, mock(ProductService.class), auditService);

        productService = mock(ProductService.class);
        saleService = new SaleService(saleRepository, productService,
            mock(CustomerService.class), movementService, auditService,
            mock(SaleNumberGenerator.class));

        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        admin = new User();
        admin.setId(1L);
        admin.setName("Admin");
        admin.setEmail("admin@delosreyes.com");

        product = new Product();
        product.setId(10L);
        product.setCode("PEU-001");
        product.setCostPrice(new BigDecimal("50.00"));
        product.setStock(7); // stock que quedó después de vender 3

        SaleItem item = new SaleItem();
        item.setProduct(product);
        item.setQuantity(3);

        sale = new Sale();
        sale.setId(100L);
        sale.setSaleNumber("VTA-20261007-00001");
        sale.setStatus(Sale.Status.COMPLETED);
        sale.setUser(admin);
        sale.addItem(item);

        when(saleRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sale));
        when(productService.findEntityByIdForUpdate(10L)).thenReturn(product);
    }

    @Test
    void cancelarDevuelveElStockUnaSolaVez() {
        saleService.cancel(100L, "Cliente desistió", admin);

        assertThat(product.getStock()).isEqualTo(10);
        assertThat(sale.getStatus()).isEqualTo(Sale.Status.CANCELLED);
    }

    @Test
    void cancelarRegistraUnaEntradaConCantidadPositiva() {
        saleService.cancel(100L, "Cliente desistió", admin);

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(captor.capture());
        InventoryMovement movement = captor.getValue();

        // La BD tiene CHECK (quantity > 0): una cantidad negativa hace fallar la cancelación
        assertThat(movement.getQuantity()).isEqualTo(3);
        assertThat(movement.getType()).isEqualTo(InventoryMovement.Type.ENTRY);
        assertThat(movement.getStockBefore()).isEqualTo(7);
        assertThat(movement.getStockAfter()).isEqualTo(10);
        assertThat(movement.getSaleId()).isEqualTo(100L);
    }

    @Test
    void noSePuedeCancelarDosVeces() {
        sale.setStatus(Sale.Status.CANCELLED);

        assertThatThrownBy(() -> saleService.cancel(100L, "Otra vez", admin))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ya está cancelada");

        assertThat(product.getStock()).isEqualTo(7);
        verify(movementRepository, never()).save(any());
    }
}
