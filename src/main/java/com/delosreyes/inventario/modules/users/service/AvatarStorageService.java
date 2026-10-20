package com.delosreyes.inventario.modules.users.service;

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
 * AvatarStorageService — Guarda fotos de perfil en disco local.
 * Mismo patrón que ProductImageService (validación, nombre único, límite
 * de 5MB), pero en su propia carpeta {app.upload.dir}/avatars/.
 */
@Service
public class AvatarStorageService {

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
            Path avatarsDir = Paths.get(uploadDir, "avatars");
            Files.createDirectories(avatarsDir);

            String extension = switch (file.getContentType()) {
                case "image/png"  -> ".png";
                case "image/webp" -> ".webp";
                default           -> ".jpg";
            };
            String filename = UUID.randomUUID() + extension;
            Path destination = avatarsDir.resolve(filename);

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/avatars/" + filename;
        } catch (IOException e) {
            throw new BusinessException("UPLOAD_FAILED", "No se pudo guardar la imagen: " + e.getMessage());
        }
    }
}