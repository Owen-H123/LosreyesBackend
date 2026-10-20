package com.delosreyes.inventario.shared.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class IgvCalculatorTest {

    @Test
    void agregaElIgvAlPrecioSinIgv() {
        assertThat(IgvCalculator.withIgv(new BigDecimal("20.00"))).isEqualByComparingTo("23.60");
        assertThat(IgvCalculator.withIgv(new BigDecimal("80.00"))).isEqualByComparingTo("94.40");
    }

    @Test
    void quitaElIgvDelPrecioConIgv() {
        assertThat(IgvCalculator.withoutIgv(new BigDecimal("23.60"))).isEqualByComparingTo("20.00");
        assertThat(IgvCalculator.withoutIgv(new BigDecimal("94.40"))).isEqualByComparingTo("80.00");
    }

    @Test
    void desglosaElIgvIncluidoEnUnTotal() {
        assertThat(IgvCalculator.includedIgv(new BigDecimal("23.60"))).isEqualByComparingTo("3.60");
        assertThat(IgvCalculator.includedIgv(new BigDecimal("37.20"))).isEqualByComparingTo("5.67");
        assertThat(IgvCalculator.includedIgv(BigDecimal.ZERO)).isEqualByComparingTo("0.00");
    }

    @Test
    void baseMasIgvSiempreDaElTotalExacto() {
        // Aunque haya redondeo, base + IGV debe sumar exactamente el total
        for (String total : new String[]{"11.79", "10.00", "0.01", "999.99", "37.20"}) {
            BigDecimal amount = new BigDecimal(total);
            BigDecimal sum = IgvCalculator.withoutIgv(amount).add(IgvCalculator.includedIgv(amount));
            assertThat(sum).as("total %s", total).isEqualByComparingTo(amount);
        }
    }
}
