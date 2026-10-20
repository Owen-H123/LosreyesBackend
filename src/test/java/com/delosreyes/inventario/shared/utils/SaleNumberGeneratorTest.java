package com.delosreyes.inventario.shared.utils;

import com.delosreyes.inventario.modules.sales.repository.SaleRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas de SaleNumberGenerator.
 *
 * La secuencia de PostgreSQL se simula con Mockito: aquí se prueba que el
 * correlativo viene de la base de datos y que el formato es el correcto.
 */
class SaleNumberGeneratorTest {

    private final String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    @Test
    void usaElCorrelativoDeLaBaseDeDatos() {
        SaleRepository repository = mock(SaleRepository.class);
        when(repository.nextSaleNumberSequence()).thenReturn(42L);

        String number = new SaleNumberGenerator(repository).generate();

        assertThat(number).isEqualTo("VTA-" + today + "-00042");
    }

    @Test
    void unaNuevaInstanciaNoVuelveAEmpezarEnUno() {
        // Simula un reinicio del backend: se crea otro generador, pero la
        // secuencia de la base de datos sigue donde se quedó.
        SaleRepository repository = mock(SaleRepository.class);
        when(repository.nextSaleNumberSequence()).thenReturn(1L, 2L, 3L);

        SaleNumberGenerator antesDelReinicio = new SaleNumberGenerator(repository);
        String primera = antesDelReinicio.generate();
        String segunda = antesDelReinicio.generate();

        SaleNumberGenerator despuesDelReinicio = new SaleNumberGenerator(repository);
        String tercera = despuesDelReinicio.generate();

        assertThat(primera).endsWith("-00001");
        assertThat(segunda).endsWith("-00002");
        assertThat(tercera).endsWith("-00003");
    }

    @Test
    void pasadoLos99999SigueSiendoUnico() {
        SaleRepository repository = mock(SaleRepository.class);
        when(repository.nextSaleNumberSequence()).thenReturn(123456L);

        String number = new SaleNumberGenerator(repository).generate();

        // sale_number es VARCHAR(20): VTA-YYYYMMDD-123456 tiene 19 caracteres
        assertThat(number).isEqualTo("VTA-" + today + "-123456");
        assertThat(number.length()).isLessThanOrEqualTo(20);
    }
}
