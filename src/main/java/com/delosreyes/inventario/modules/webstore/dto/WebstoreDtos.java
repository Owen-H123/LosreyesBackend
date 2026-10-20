package com.delosreyes.inventario.modules.webstore.dto;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import com.delosreyes.inventario.modules.products.entity.Product;
import com.delosreyes.inventario.modules.webstore.entity.StoreSettings;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Entradas y salidas de los ajustes y del catálogo de la tienda online. */
public final class WebstoreDtos {

    private WebstoreDtos() {}

    public static final int MAX_PAYMENT_METHODS = 8;

    // ── Ajustes ───────────────────────────────────────────────

    public record SettingsResponse(
        String whatsapp, String announcement, List<String> paymentMethods,
        String pickupAddress, String businessHours, String shippingNote,
        String yapeNumber, String yapeHolder, String yapeQrUrl) {

        public static SettingsResponse from(StoreSettings s) {
            List<String> methods = Arrays.stream(s.getPaymentMethods().split(","))
                .map(String::trim).filter(m -> !m.isEmpty()).toList();
            return new SettingsResponse(s.getWhatsapp(), s.getAnnouncement(), methods,
                s.getPickupAddress(), s.getBusinessHours(), s.getShippingNote(),
                s.getYapeNumber(), s.getYapeHolder(), s.getYapeQrUrl());
        }
    }

    public record SettingsRequest(
        @Pattern(regexp = "^$|^[0-9]{9,15}$", message = "WhatsApp: solo dígitos con código de país (ej. 51987654321)") String whatsapp,
        @Size(max = 200) String announcement,
        @NotNull @Size(min = 1, max = MAX_PAYMENT_METHODS, message = "Indica entre 1 y 8 medios de pago") List<@Size(min = 1, max = 40) String> paymentMethods,
        @Size(max = 255) String pickupAddress,
        @Size(max = 150) String businessHours,
        @Size(max = 255) String shippingNote,
        @Pattern(regexp = "^$|^[0-9]{9}$", message = "El número de Yape son 9 dígitos") String yapeNumber,
        @Size(max = 100) String yapeHolder,
        // Solo una imagen propia (/uploads/...) o una URL https: nada de javascript: ni data:
        @Pattern(regexp = "^$|^(https://|/uploads/).{1,480}$", message = "La imagen del QR debe ser una URL https o una ruta /uploads") String yapeQrUrl) {}

    // ── Catálogo web ──────────────────────────────────────────

    public record CatalogItem(
        Long id, String code, String name, String brand, String categoryName,
        BigDecimal price, Integer stock, String imageUrl, boolean published, boolean featured) {

        public static CatalogItem from(Product p) {
            return new CatalogItem(p.getId(), p.getCode(), p.getName(), p.getBrand(),
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getSalePrice(), p.getStock(), p.getImageUrl(), p.isWebPublished(), p.isWebFeatured());
        }
    }

    public record CatalogUpdate(Boolean published, Boolean featured) {}

    /** Filtros del catálogo web; todos opcionales. */
    public record CatalogFilter(String search, Boolean published, boolean featured) {}
}
