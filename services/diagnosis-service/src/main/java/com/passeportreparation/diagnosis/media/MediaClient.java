package com.passeportreparation.diagnosis.media;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class MediaClient {

    @Value("${app.media.service-url:http://localhost:8083}")
    private String mediaServiceUrl;

    public MediaImage fetch(String mediaId) {
        RestClient client = RestClient.builder()
                .baseUrl(mediaServiceUrl.replaceAll("/$", ""))
                .build();

        ResponseEntity<byte[]> response = client.get()
                .uri("/api/media/{id}", mediaId)
                .retrieve()
                .toEntity(byte[].class);

        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            throw new MediaFetchException("Média introuvable ou vide: " + mediaId);
        }
        String contentType = response.getHeaders().getFirst("Content-Type");
        return new MediaImage(mediaId, body, contentType == null ? "image/jpeg" : contentType);
    }

    public record MediaImage(String mediaId, byte[] bytes, String contentType) {
    }
}
