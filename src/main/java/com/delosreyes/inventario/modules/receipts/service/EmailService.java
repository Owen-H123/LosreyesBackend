package com.delosreyes.inventario.modules.receipts.service;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.delosreyes.inventario.modules.receipts.dto.ReceiptDtos;

/**
 * EmailService — Envío de boletas en PDF por correo, vía la API HTTP de Resend
 * (https://resend.com). No usa SMTP ni contraseñas de Gmail — solo una API key.
 *
 * CONFIGURACIÓN REQUERIDA (ver application.properties):
 *   resend.api-key              → variable de entorno RESEND_API_KEY (empieza con "re_")
 *   resend.from                 → remitente. "onboarding@resend.dev" mientras no
 *                                  haya un dominio propio verificado en Resend.
 *   app.email.test-mode         → true mientras no haya dominio verificado
 *   app.email.test-recipient    → variable de entorno TEST_EMAIL_RECIPIENT
 *                                  (tu correo verificado en Resend). En modo
 *                                  prueba, TODOS los correos llegan aquí,
 *                                  sin importar el email real del cliente
 *                                  (restricción de Resend en cuentas sin
 *                                  dominio verificado, para evitar spam).
 *
 * CUANDO TENGAS UN DOMINIO PROPIO VERIFICADO EN RESEND:
 *   1. Cambia resend.from a algo como boletas@tudominio.com
 *   2. Cambia app.email.test-mode a false
 *   Y los correos empezarán a llegar directo al cliente real, sin tocar código.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from}")
    private String fromAddress;

    @Value("${app.email.test-mode:true}")
    private boolean testMode;

    @Value("${app.email.test-recipient:}")
    private String testRecipient;

    /**
     * Envía la boleta en PDF al correo del cliente (o al correo de prueba,
     * si app.email.test-mode está activo).
     *
     * @param data     datos de la boleta (asunto/cuerpo del correo)
     * @param pdfBytes PDF ya generado, se adjunta al correo
     * @return true si Resend aceptó el envío, false si el cliente no tiene
     *         email registrado o la API de Resend rechazó la solicitud.
     */
    public boolean sendReceiptEmail(ReceiptDtos.ReceiptData data, byte[] pdfBytes) {

        String customerEmail = data.getCustomerEmail();

        if (customerEmail == null || customerEmail.isBlank()) {
            log.warn("No se pudo enviar la boleta {}: el cliente no tiene email registrado",
                data.getSaleNumber());
            return false;
        }

        boolean usingTestRecipient = testMode && testRecipient != null && !testRecipient.isBlank();
        String recipient = usingTestRecipient ? testRecipient : customerEmail;

        String subject = usingTestRecipient
            ? "[PRUEBA - iba para " + customerEmail + "] Boleta " + data.getSaleNumber() + " - De Los Reyes S.A.C"
            : "Boleta de venta " + data.getSaleNumber() + " - De Los Reyes S.A.C";

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("from", fromAddress);
            body.put("to", List.of(recipient));
            body.put("subject", subject);
            body.put("html", buildEmailBody(data, usingTestRecipient, customerEmail));
            body.put("attachments", List.of(buildAttachment(data, pdfBytes)));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            var response = restTemplate.postForEntity(RESEND_API_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Boleta {} enviada por correo a {} (destinatario real: {})",
                    data.getSaleNumber(), recipient, customerEmail);
                return true;
            }

            log.error("Resend respondió con estado inesperado {} al enviar la boleta {}",
                response.getStatusCode(), data.getSaleNumber());
            return false;

        } catch (RestClientException ex) {
            log.error("Error enviando la boleta {} a {} vía Resend: {}",
                data.getSaleNumber(), recipient, ex.getMessage());
            return false;
        }
    }

    private Map<String, Object> buildAttachment(ReceiptDtos.ReceiptData data, byte[] pdfBytes) {
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("filename", "boleta-" + data.getSaleNumber() + ".pdf");
        attachment.put("content", Base64.getEncoder().encodeToString(pdfBytes));
        return attachment;
    }

    /**
     * Envía el enlace para restablecer la contraseña.
     *
     * MODO PRUEBA (app.email.test-mode=true): además de redirigir el correo a
     * app.email.test-recipient, el enlace se escribe en el log del backend para
     * poder probar el flujo sin una cuenta de Resend configurada. Nunca debe
     * usarse así en producción: el log tendría enlaces válidos.
     *
     * @return true si Resend aceptó el envío
     */
    public boolean sendPasswordResetEmail(String userEmail, String userName, String resetLink, int minutesValid) {
        if (testMode) {
            log.warn("[MODO PRUEBA] Enlace de recuperación para {}: {}", userEmail, resetLink);
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("No se envió el correo de recuperación a {}: falta RESEND_API_KEY", userEmail);
            return false;
        }

        boolean usingTestRecipient = testMode && testRecipient != null && !testRecipient.isBlank();
        String recipient = usingTestRecipient ? testRecipient : userEmail;
        String subject = usingTestRecipient
            ? "[PRUEBA - iba para " + userEmail + "] Restablecer contraseña - De Los Reyes S.A.C"
            : "Restablecer contraseña - De Los Reyes S.A.C";

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("from", fromAddress);
            body.put("to", List.of(recipient));
            body.put("subject", subject);
            body.put("html", buildPasswordResetBody(userName, resetLink, minutesValid,
                usingTestRecipient, userEmail));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            var response = restTemplate.postForEntity(RESEND_API_URL, new HttpEntity<>(body, headers), String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Correo de recuperación enviado a {} (destinatario real: {})", recipient, userEmail);
                return true;
            }
            log.error("Resend respondió con estado inesperado {} al enviar la recuperación a {}",
                response.getStatusCode(), userEmail);
            return false;
        } catch (RestClientException ex) {
            log.error("Error enviando el correo de recuperación a {} vía Resend: {}", recipient, ex.getMessage());
            return false;
        }
    }

    private String buildPasswordResetBody(String userName, String resetLink, int minutesValid,
                                          boolean isTest, String realUserEmail) {
        String testNotice = isTest
            ? "<p style=\"background:#fef3c7;color:#92400e;padding:8px 12px;border-radius:6px;font-size:13px;\">"
              + "Modo prueba: este correo iba dirigido a <strong>" + realUserEmail + "</strong>, "
              + "pero se redirigió aquí porque todavía no hay un dominio verificado en Resend.</p>"
            : "";

        return """
            <div style="font-family: Arial, sans-serif; color: #1e293b; max-width: 480px; margin: 0 auto;">
                %s
                <h2 style="margin-bottom: 4px;">De Los Reyes S.A.C</h2>
                <p>Hola %s,</p>
                <p>Recibimos una solicitud para restablecer tu contraseña del sistema.</p>
                <p style="margin: 24px 0;">
                    <a href="%s" style="background:#1d4ed8;color:#fff;padding:12px 20px;border-radius:8px;text-decoration:none;font-weight:bold;">
                        Crear nueva contraseña
                    </a>
                </p>
                <p style="color:#64748b;font-size:13px;">El enlace vence en %d minutos y solo se puede usar una vez.</p>
                <p style="color:#64748b;font-size:13px;">Si no pediste este cambio, ignora este correo: tu contraseña sigue siendo la misma.</p>
            </div>
            """.formatted(testNotice, userName, resetLink, minutesValid);
    }

    private String buildEmailBody(ReceiptDtos.ReceiptData data, boolean isTest, String realCustomerEmail) {
        String testNotice = isTest
            ? "<p style=\"background:#fef3c7;color:#92400e;padding:8px 12px;border-radius:6px;font-size:13px;\">"
              + "Modo prueba: este correo iba dirigido a <strong>" + realCustomerEmail + "</strong>, "
              + "pero se redirigió aquí porque todavía no hay un dominio verificado en Resend.</p>"
            : "";

        return """
            <div style="font-family: Arial, sans-serif; color: #1e293b; max-width: 480px; margin: 0 auto;">
                %s
                <h2 style="margin-bottom: 4px;">De Los Reyes S.A.C</h2>
                <p style="color: #64748b; margin-top: 0;">Repuestos y accesorios automotrices</p>
                <p>Hola %s,</p>
                <p>Adjuntamos la boleta de tu compra <strong>%s</strong> realizada el %s.</p>
                <p><strong>Total pagado:</strong> S/ %s</p>
                <p>Gracias por tu preferencia.</p>
            </div>
            """.formatted(
                testNotice,
                data.getCustomerName(),
                data.getSaleNumber(),
                data.getSaleDate() != null ? data.getSaleDate().toLocalDate() : "-",
                data.getTotal() != null ? data.getTotal().toPlainString() : "-"
            );
    }
}