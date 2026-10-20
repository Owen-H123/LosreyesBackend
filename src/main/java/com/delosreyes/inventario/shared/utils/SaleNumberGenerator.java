package com.delosreyes.inventario.shared.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.delosreyes.inventario.modules.sales.repository.SaleRepository;
import com.delosreyes.inventario.shared.constants.AppConstants;

/**
 * SaleNumberGenerator — Genera números de venta únicos y legibles.
 *
 * QUÉ HACE:
 *   Genera números del tipo: VTA-20240115-00001
 *   Formato: {PREFIX}-{YYYYMMDD}-{CORRELATIVO de al menos 5 dígitos}
 *
 * POR QUÉ EXISTE:
 *   Reemplaza IDs numéricos con números legibles para el negocio.
 *   El personal puede referenciar ventas verbalmente.
 *
 * DE DÓNDE SALE EL CORRELATIVO:
 *   De la secuencia sale_number_seq de PostgreSQL (migración V5). La base de
 *   datos recuerda el último valor aunque se reinicie el backend, y nextval()
 *   nunca entrega el mismo número dos veces, aunque haya ventas simultáneas.
 *   El correlativo es global: no vuelve a 1 cada día.
 */
@Component
public class SaleNumberGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SaleRepository saleRepository;

    public SaleNumberGenerator(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public String generate() {
        String date = LocalDate.now().format(DATE_FMT);
        long seq = saleRepository.nextSaleNumberSequence();
        return String.format("%s-%s-%05d", AppConstants.SALE_PREFIX, date, seq);
    }
}
