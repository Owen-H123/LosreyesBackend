package com.delosreyes.inventario.shared.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * IgvCalculator — Cálculos del IGV (18 %) en un solo lugar.
 *
 * CRITERIO DEL SISTEMA:
 *   Los precios de venta de los productos se guardan CON IGV incluido, que es
 *   el precio que paga el cliente. El IGV de una venta no se suma al final:
 *   se desglosa del total.
 *
 * EJEMPLO:
 *   Precio sin IGV S/ 20.00  →  withIgv  →  S/ 23.60
 *   Total S/ 23.60           →  includedIgv  →  S/ 3.60
 *
 * Todos los montos se redondean a céntimos con HALF_UP.
 */
public final class IgvCalculator {

    public static final BigDecimal RATE = new BigDecimal("0.18");
    private static final BigDecimal FACTOR = BigDecimal.ONE.add(RATE); // 1.18

    private IgvCalculator() {
    }

    /** Precio sin IGV → precio con IGV. */
    public static BigDecimal withIgv(BigDecimal priceWithoutIgv) {
        return priceWithoutIgv.multiply(FACTOR).setScale(2, RoundingMode.HALF_UP);
    }

    /** Precio con IGV → precio sin IGV. */
    public static BigDecimal withoutIgv(BigDecimal priceWithIgv) {
        return priceWithIgv.divide(FACTOR, 2, RoundingMode.HALF_UP);
    }

    /** Parte del monto (que ya incluye IGV) que corresponde al IGV. */
    public static BigDecimal includedIgv(BigDecimal amountWithIgv) {
        return amountWithIgv.setScale(2, RoundingMode.HALF_UP).subtract(withoutIgv(amountWithIgv));
    }
}
