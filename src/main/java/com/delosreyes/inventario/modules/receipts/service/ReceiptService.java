package com.delosreyes.inventario.modules.receipts.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delosreyes.inventario.modules.receipts.dto.ReceiptDtos;
import com.delosreyes.inventario.modules.sales.entity.Sale;
import com.delosreyes.inventario.modules.sales.entity.SaleItem;
import com.delosreyes.inventario.modules.sales.repository.SaleRepository;
import com.delosreyes.inventario.shared.exceptions.BusinessException;
import com.delosreyes.inventario.shared.exceptions.ResourceNotFoundException;

/**
 * ReceiptService — Orquesta la generación de boletas a partir de una venta existente.
 *
 * QUÉ HACE:
 *   1. Busca la venta (con cliente, usuario y productos ya asociados).
 *   2. Transforma la entidad Sale en un ReceiptDtos.ReceiptData (DTO de presentación).
 *   3. Delega la construcción del PDF a PdfGeneratorService.
 *   4. Delega el envío por correo a EmailService.
 *
 * POR QUÉ NO DUPLICA LÓGICA DE SaleService:
 *   No recalcula totales ni valida stock: solo lee datos ya persistidos
 *   directamente desde SaleRepository (misma fuente de verdad que SaleService).
 */
@Service
@Transactional(readOnly = true)
public class ReceiptService {

    private final SaleRepository saleRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final EmailService emailService;

    public ReceiptService(SaleRepository saleRepository,
                           PdfGeneratorService pdfGeneratorService,
                           EmailService emailService) {
        this.saleRepository = saleRepository;
        this.pdfGeneratorService = pdfGeneratorService;
        this.emailService = emailService;
    }

    /**
     * Obtiene la venta y arma el DTO con toda la información necesaria
     * para imprimir la boleta (cliente, usuario, productos, totales).
     */
    public ReceiptDtos.ReceiptData buildReceiptData(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
            .orElseThrow(() -> new ResourceNotFoundException("Venta", "id", saleId));

        return toReceiptData(sale);
    }

    /**
     * Genera los bytes del PDF de la boleta para la venta indicada.
     */
    public byte[] generatePdf(ReceiptDtos.ReceiptData data) {
        return pdfGeneratorService.generateReceiptPdf(data);
    }

    /**
     * Atajo: obtiene la venta y genera el PDF en un solo paso.
     */
    public byte[] generatePdfForSale(Long saleId) {
        return generatePdf(buildReceiptData(saleId));
    }

    /**
     * Genera el PDF de la boleta y lo envía al correo del cliente.
     * Lanza BusinessException si el cliente no tiene correo registrado
     * o si el envío falla (SMTP caído, credenciales inválidas, etc.).
     */
    public void sendReceiptByEmail(Long saleId) {
        ReceiptDtos.ReceiptData data = buildReceiptData(saleId);
        byte[] pdfBytes = pdfGeneratorService.generateReceiptPdf(data);

        boolean sent = emailService.sendReceiptEmail(data, pdfBytes);

        if (!sent) {
            throw new BusinessException("RECEIPT_EMAIL_FAILED",
                "No se pudo enviar la boleta por correo. Verifica que el cliente "
                + "tenga un email registrado y que la configuración SMTP sea correcta.");
        }
    }

    // ============================================================
    // Mapeo Sale -> ReceiptData
    // ============================================================
    private ReceiptDtos.ReceiptData toReceiptData(Sale sale) {
        ReceiptDtos.ReceiptData data = new ReceiptDtos.ReceiptData();

        data.setSaleId(sale.getId());
        data.setSaleNumber(sale.getSaleNumber());
        data.setSaleDate(sale.getSaleDate());
        data.setStatus(sale.getStatus().name());
        data.setPaymentMethod(sale.getPaymentMethod().name());
        data.setSubtotal(sale.getSubtotal());
        data.setDiscount(sale.getDiscount());
        data.setTax(sale.getTax());
        data.setTotal(sale.getTotal());
        data.setNotes(sale.getNotes());

        data.setSellerName(sale.getUser().getName());
        data.setSellerEmail(sale.getUser().getEmail());

        if (sale.getCustomer() != null) {
            data.setCustomerName(sale.getCustomer().getName());
            data.setCustomerDocumentType(sale.getCustomer().getDocumentType().name());
            data.setCustomerDocumentNumber(sale.getCustomer().getDocumentNumber());
            data.setCustomerEmail(sale.getCustomer().getEmail());
            data.setCustomerAddress(sale.getCustomer().getAddress());
        } else {
            data.setCustomerName("Público general");
            data.setCustomerDocumentType("-");
            data.setCustomerDocumentNumber("-");
        }

        List<ReceiptDtos.ReceiptItem> items = sale.getItems().stream()
            .map(this::toReceiptItem)
            .collect(Collectors.toList());
        data.setItems(items);

        return data;
    }

    private ReceiptDtos.ReceiptItem toReceiptItem(SaleItem item) {
        ReceiptDtos.ReceiptItem ri = new ReceiptDtos.ReceiptItem();
        ri.setProductCode(item.getProduct().getCode());
        ri.setProductName(item.getProduct().getName());
        ri.setQuantity(item.getQuantity());
        ri.setUnitPrice(item.getUnitPrice());
        ri.setDiscount(item.getDiscount());
        ri.setSubtotal(item.getSubtotal());
        return ri;
    }
}