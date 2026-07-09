package com.passeportreparation.media.service;

import com.passeportreparation.common.dto.MediaUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class MediaStorageService {

    private static final String IMAGE_WEBP = "image/webp";

    private static final Set<String> ALLOWED_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            IMAGE_WEBP
    );

    private final Path storageRoot;
    private final String publicBaseUrl;

    public MediaStorageService(
            @Value("${app.media.storage-path:./data/media}") String storagePath,
            @Value("${app.media.public-base-url:http://localhost:8083}") String publicBaseUrl
    ) throws IOException {
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "");
        Files.createDirectories(this.storageRoot);
        log.info("Media storage root: {}", this.storageRoot);
    }

    public MediaUploadResponse store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MediaValidationException("Fichier vide");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new MediaValidationException("Type non supporté (JPEG, PNG, WEBP uniquement)");
        }

        String mediaId = UUID.randomUUID().toString();
        String extension = extensionFor(contentType);
        Path target = storageRoot.resolve(mediaId + extension);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new MediaStorageException("Échec de l'enregistrement du fichier", e);
        }

        return MediaUploadResponse.builder()
                .mediaId(mediaId)
                .contentType(contentType)
                .sizeBytes(file.getSize())
                .url(publicBaseUrl + "/api/media/" + mediaId)
                .build();
    }

    public Resource loadAsResource(String mediaId) {
        try {
            Path file = findFile(mediaId);
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new MediaNotFoundException(mediaId);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new MediaNotFoundException(mediaId);
        }
    }

    public String detectContentType(String mediaId) {
        Path file = findFile(mediaId);
        try {
            String type = Files.probeContentType(file);
            return type != null ? type : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        } catch (IOException e) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }

    private Path findFile(String mediaId) {
        if (mediaId == null || mediaId.isBlank() || mediaId.contains("..") || mediaId.contains("/")) {
            throw new MediaNotFoundException(mediaId);
        }
        try (var stream = Files.list(storageRoot)) {
            return stream
                    .filter(p -> p.getFileName().toString().startsWith(mediaId))
                    .findFirst()
                    .orElseThrow(() -> new MediaNotFoundException(mediaId));
        } catch (IOException e) {
            throw new MediaNotFoundException(mediaId);
        }
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case MediaType.IMAGE_PNG_VALUE -> ".png";
            case IMAGE_WEBP -> ".webp";
            default -> ".jpg";
        };
    }
}
