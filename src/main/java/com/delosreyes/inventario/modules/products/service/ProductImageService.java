package com.delosreyes.inventario.modules.products.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.delosreyes.inventario.shared.exceptions.BusinessException;

/**
 * ProductImageService — Guarda imágenes de productos en disco local.
 *
 * Valida tipo (jpg/png/webp) y tamaño (5MB máx, igual al límite de
 * spring.servlet.multipart.max-file-size), genera un nombre único (UUID)
 * para evitar colisiones, y guarda el archivo en {app.upload.dir}/products/.
 *
 * No borra la imagen anterior del disco al reemplazarla — se acepta el
 * archivo huérfano como costo aceptable a cambio de simplicidad. Si el
 * disco crece mucho con el tiempo, se puede agregar limpieza más adelante.
 */
@Service
public class ProductImageService {

    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "No se recibió ningún archivo");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessException("INVALID_FILE_TYPE", "Solo se permiten imágenes JPG, PNG o WEBP");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException("FILE_TOO_LARGE", "La imagen no puede pesar más de 5MB");
        }

        try {
            Path productsDir = Paths.get(uploadDir, "products");
            Files.createDirectories(productsDir);

            String extension = switch (file.getContentType()) {
                case "image/png"  -> ".png";
                case "image/webp" -> ".webp";
                default           -> ".jpg";
            };
            String filename = UUID.randomUUID() + extension;
            Path destination = productsDir.resolve(filename);

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/products/" + filename;
        } catch (IOException e) {
            throw new BusinessException("UPLOAD_FAILED", "No se pudo guardar la imagen: " + e.getMessage());
        }
    }
}