package com.delosreyes.inventario.modules.webstore.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delosreyes.inventario.modules.webstore.dto.WebstoreDtos.SettingsRequest;
import com.delosreyes.inventario.modules.webstore.dto.WebstoreDtos.SettingsResponse;
import com.delosreyes.inventario.modules.webstore.entity.StoreSettings;
import com.delosreyes.inventario.modules.webstore.repository.StoreSettingsRepository;

/** Ajustes de la tienda online. Es una sola fila (id = 1) que la web lee en vivo. */
@Service
public class StoreSettingsService {

    private static final String DEFAULT_PAYMENT_METHODS = "Yape / Plin,Transferencia,Efectivo al recoger";

    private final StoreSettingsRepository repository;

    public StoreSettingsService(StoreSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SettingsResponse get() {
        return SettingsResponse.from(load());
    }

    @Transactional
    public SettingsResponse save(SettingsRequest req) {
        StoreSettings s = load();
        s.setWhatsapp(blankToNull(req.whatsapp()));
        s.setAnnouncement(blankToNull(req.announcement()));
        // Se guardan separados por coma: una coma dentro de un nombre lo partiría en dos
        s.setPaymentMethods(String.join(",", req.paymentMethods().stream()
            .map(m -> m.replace(",", " ").trim()).filter(m -> !m.isEmpty()).distinct().toList()));
        s.setPickupAddress(blankToNull(req.pickupAddress()));
        s.setBusinessHours(blankToNull(req.businessHours()));
        s.setShippingNote(blankToNull(req.shippingNote()));
        s.setYapeNumber(blankToNull(req.yapeNumber()));
        s.setYapeHolder(blankToNull(req.yapeHolder()));
        s.setYapeQrUrl(blankToNull(req.yapeQrUrl()));
        return SettingsResponse.from(repository.save(s));
    }

    private StoreSettings load() {
        return repository.findById(StoreSettings.SINGLETON_ID).orElseGet(() -> {
            StoreSettings s = new StoreSettings();
            s.setPaymentMethods(DEFAULT_PAYMENT_METHODS);
            return repository.save(s);
        });
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
