package com.delosreyes.inventario.modules.receipts.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delosreyes.inventario.modules.receipts.dto.ReceiptDtos;
import com.delosreyes.inventario.modules.receipts.service.ReceiptService;
import com.delosreyes.inventario.shared.responses.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * ReceiptController — Endpoints REST del módulo de boletas.
 *
 * ACCESO:
 *   GET  /receipts/{saleId}/pdf   — descarga el PDF (ADMIN + WORKER)
 *   POST /receipts/{saleId}/email — envía el PDF por correo al cliente (ADMIN + WORKER)
 *
 * RUTA FINAL (con server.servlet.context-path=/api/v1):
 *   GET  /api/v1/receipts/{saleId}/pdf
 *   POST /api/v1/receipts/{saleId}/email
 *
 * NOTA — por qué GET /pdf NO declara "produces":
 *   Ver comentario histórico: fijar produces=APPLICATION_PDF_VALUE hace que
 *   cualquier excepción termine en HttpMediaTypeNotAcceptableException al
 *   intentar responder JSON desde GlobalExceptionHandler. El Content-Type
 *   real ya se fija manualmente en los headers de la respuesta exitosa.
 */
@RestController
@RequestMapping("/receipts")
@Tag(name = "Boletas", description = "Generación y envío de boletas de venta en PDF")
@SecurityRequirement(name = "Bearer Authentication")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping("/{saleId}/pdf")
    @Operation(summary = "Generar y descargar la boleta de una venta en formato PDF")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long saleId) {

        ReceiptDtos.ReceiptData data = receiptService.buildReceiptData(saleId);
        byte[] pdfBytes = receiptService.generatePdf(data);

        String fileName = "boleta-" + data.getSaleNumber() + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
            ContentDisposition.attachment()
                .filename(fileName)
                .build()
        );
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body(pdfBytes);
    }

    @PostMapping("/{saleId}/email")
    @Operation(summary = "Enviar la boleta de una venta por correo electrónico al cliente")
    public ResponseEntity<ApiResponse<Void>> sendByEmail(@PathVariable Long saleId) {
        receiptService.sendReceiptByEmail(saleId);
        return ResponseEntity.ok(ApiResponse.success("Boleta enviada por correo exitosamente"));
    }
}