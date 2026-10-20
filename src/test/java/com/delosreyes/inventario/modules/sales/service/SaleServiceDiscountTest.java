package com.delosreyes.inventario.modules.sales.service;

import com.delosreyes.inventario.modules.audit.service.AuditService;
import com.delosreyes.inventario.modules.customers.service.CustomerService;
import com.delosreyes.inventario.modules.movements.service.InventoryMovementService;
import com.delosreyes.inventario.modules.products.entity.Product;
import com.delosreyes.inventario.modules.products.service.ProductService;
import com.delosreyes.inventario.modules.sales.dto.SaleDtos;
import com.delosreyes.inventario.modules.sales.repository.SaleRepository;
import com.delosreyes.inventario.modules.users.entity.User;
import com.delosreyes.inventario.shared.exceptions.BusinessException;
import com.delosreyes.inventario.shared.utils.SaleNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de los topes de descuento en SaleService.create().
 *
 * Producto de prueba: S/ 80.00 (con IGV) y stock 10. Una línea de 2 unidades vale S/ 160.00.
 */
class SaleServiceDiscountTest {

    private SaleRepository saleRepository;
    private SaleService saleService;
    private User admin;
    private Product product;

    @BeforeEach
    void setUp() {
        saleRepository = mock(SaleRepository.class);
        ProductService productService = mock(ProductService.class);
        SaleNumberGenerator saleNumberGenerator = mock(SaleNumberGenerator.class);

        saleService = new SaleService(saleRepository, productService, mock(CustomerService.class),
            mock(InventoryMovementService.class), mock(AuditService.class), saleNumberGenerator);

        product = new Product();
        product.setId(10L);
        product.setCode("PEU-001");
        product.setSalePrice(new BigDecimal("80.00"));
        product.setCostPrice(new BigDecimal("50.00"));
        product.setStock(10);

        admin = new User();
        admin.setId(1L);
        admin.setName("Admin");
        admin.setEmail("admin@delosreyes.com");

        when(productService.findEntityByIdForUpdate(10L)).thenReturn(product);
        when(saleNumberGenerator.generate()).thenReturn("VTA-20261008-00001");
        when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void descuentoDeItemMayorAlImporteSeRechaza() {
        var request = sale(item("160.01"), "0");

        assertThatThrownBy(() -> saleService.create(request, admin))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("no puede ser mayor que el importe del producto");

        verify(saleRepository, never()).save(any());
    }

    @Test
    void descuentoDeItemDel100PorCientoSePermite() {
        var response = saleService.create(sale(item("160.00"), "0"), admin);

        assertThat(response.getSubtotal()).isEqualByComparingTo("0.00");
        assertThat(response.getTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void descuentoGlobalMayorAlSubtotalSeRechaza() {
        var request = sale(item("0"), "160.01");

        assertThatThrownBy(() -> saleService.create(request, admin))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("no puede ser mayor que el subtotal");

        verify(saleRepository, never()).save(any());
    }

    @Test
    void descuentoGlobalIgualAlSubtotalDejaTotalCero() {
        var response = saleService.create(sale(item("0"), "160.00"), admin);

        assertThat(response.getTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void descuentosValidosCalculanBienElTotal() {
        // Los precios ya incluyen IGV: el IGV se desglosa del total, no se suma
        // 160.00 - 10.00 (item) = 150.00 de subtotal
        // 150.00 - 50.00 (global) = 100.00 de total; IGV incluido 100.00 - 84.75 = 15.25
        var response = saleService.create(sale(item("10.00"), "50.00"), admin);

        assertThat(response.getSubtotal()).isEqualByComparingTo("150.00");
        assertThat(response.getTotal()).isEqualByComparingTo("100.00");
        assertThat(response.getTax()).isEqualByComparingTo("15.25");
    }

    @Test
    void elDescuentoBajaElTotalExactamenteLoQueEscribeElCajero() {
        // Ejemplo acordado con la empresa: 2 x S/ 23.60 = S/ 47.20, descuento S/ 10.00
        product.setSalePrice(new BigDecimal("23.60"));

        var response = saleService.create(sale(item("0"), "10.00"), admin);

        assertThat(response.getSubtotal()).isEqualByComparingTo("47.20");
        assertThat(response.getTotal()).isEqualByComparingTo("37.20");
        assertThat(response.getTax()).isEqualByComparingTo("5.67");
    }

    // 2 unidades del producto 10 con el descuento indicado
    private SaleDtos.ItemRequest item(String discount) {
        SaleDtos.ItemRequest item = new SaleDtos.ItemRequest();
        item.setProductId(10L);
        item.setQuantity(2);
        item.setDiscount(new BigDecimal(discount));
        return item;
    }

    private SaleDtos.CreateRequest sale(SaleDtos.ItemRequest item, String globalDiscount) {
        SaleDtos.CreateRequest request = new SaleDtos.CreateRequest();
        request.setItems(List.of(item));
        request.setGlobalDiscount(new BigDecimal(globalDiscount));
        return request;
    }
}
