package com.delosreyes.inventario.modules.sales.service;

import com.delosreyes.inventario.modules.sales.dto.SaleDtos;
import com.delosreyes.inventario.modules.users.entity.User;
import com.delosreyes.inventario.modules.users.repository.UserRepository;
import com.delosreyes.inventario.shared.exceptions.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de concurrencia de ventas contra un PostgreSQL real.
 *
 * El bloqueo de filas (SELECT ... FOR UPDATE) lo hace la base de datos, así que
 * estas pruebas no se pueden hacer con H2 ni con Mockito. Testcontainers levanta
 * un PostgreSQL temporal en Docker, Flyway aplica las migraciones y al terminar
 * el contenedor se elimina. La base de datos de desarrollo no se toca.
 */
@SpringBootTest
@Testcontainers
class SaleConcurrencyTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private SaleService saleService;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbc;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = userRepository.findByEmail("admin@delosreyes.com").orElseThrow();
    }

    @Test
    void diezVentasSimultaneasConStock5VendenExactamente5() throws Exception {
        long productId = createProduct("CONC-001", 5);

        List<Result> results = runConcurrently(10, () -> saleService.create(sale(item(productId, 1)), admin));

        assertThat(count(results, Result.OK)).isEqualTo(5);
        assertThat(count(results, Result.INSUFFICIENT_STOCK)).isEqualTo(5);
        assertThat(count(results, Result.OTHER_ERROR)).isZero();
        assertThat(stockOf(productId)).isZero();
        assertThat(saleMovementsOf(productId)).isEqualTo(5);
    }

    @Test
    void ventasConLosMismosProductosEnDistintoOrdenNoSeBloqueanEntreSi() throws Exception {
        long a = createProduct("CONC-A", 100);
        long b = createProduct("CONC-B", 100);
        AtomicInteger turn = new AtomicInteger();

        // La mitad compra [A, B] y la otra mitad [B, A]
        List<Result> results = runConcurrently(10, () -> turn.getAndIncrement() % 2 == 0
            ? saleService.create(sale(item(a, 1), item(b, 1)), admin)
            : saleService.create(sale(item(b, 1), item(a, 1)), admin));

        assertThat(count(results, Result.OK)).isEqualTo(10);
        assertThat(stockOf(a)).isEqualTo(90);
        assertThat(stockOf(b)).isEqualTo(90);
    }

    @Test
    void dosCancelacionesSimultaneasDevuelvenElStockUnaSolaVez() throws Exception {
        long productId = createProduct("CONC-CANCEL", 10);
        Long saleId = saleService.create(sale(item(productId, 4)), admin).getId();
        assertThat(stockOf(productId)).isEqualTo(6);

        List<Result> results = runConcurrently(2, () -> saleService.cancel(saleId, "Prueba concurrente", admin));

        assertThat(count(results, Result.OK)).isEqualTo(1);
        assertThat(count(results, Result.ALREADY_CANCELLED)).isEqualTo(1);
        assertThat(stockOf(productId)).isEqualTo(10);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private enum Result { OK, INSUFFICIENT_STOCK, ALREADY_CANCELLED, OTHER_ERROR }

    /**
     * Lanza la misma operación en varios hilos a la vez. El CountDownLatch hace
     * que todos esperen en la "línea de partida" y arranquen juntos.
     */
    private List<Result> runConcurrently(int threads, Callable<?> operation) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Result>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    operation.call();
                    return Result.OK;
                } catch (BusinessException e) {
                    if ("INSUFFICIENT_STOCK".equals(e.getErrorCode())) return Result.INSUFFICIENT_STOCK;
                    if ("ALREADY_CANCELLED".equals(e.getErrorCode())) return Result.ALREADY_CANCELLED;
                    return Result.OTHER_ERROR;
                } catch (Exception e) {
                    return Result.OTHER_ERROR;
                }
            }));
        }
        start.countDown();
        List<Result> results = new ArrayList<>();
        for (Future<Result> f : futures) results.add(f.get(60, TimeUnit.SECONDS));
        pool.shutdown();
        return results;
    }

    private long count(List<Result> results, Result expected) {
        return results.stream().filter(r -> r == expected).count();
    }

    private long createProduct(String code, int stock) {
        return jdbc.queryForObject("""
            INSERT INTO products (code, name, category_id, cost_price, sale_price, stock, minimum_stock)
            VALUES (?, ?, (SELECT MIN(id) FROM categories), 50, 80, ?, 0)
            RETURNING id
            """, Long.class, code, "Producto " + code, stock);
    }

    private int stockOf(long productId) {
        return jdbc.queryForObject("SELECT stock FROM products WHERE id = ?", Integer.class, productId);
    }

    private int saleMovementsOf(long productId) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM inventory_movements WHERE product_id = ? AND type = 'SALE'",
            Integer.class, productId);
    }

    private SaleDtos.ItemRequest item(long productId, int quantity) {
        SaleDtos.ItemRequest item = new SaleDtos.ItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    private SaleDtos.CreateRequest sale(SaleDtos.ItemRequest... items) {
        SaleDtos.CreateRequest request = new SaleDtos.CreateRequest();
        request.setItems(List.of(items));
        return request;
    }
}
