package com.duoc.eft.bff.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class InscripcionesClient {
    private final RestClient client;
    public InscripcionesClient(RestClient.Builder builder, @Value("${app.services.inscripciones-url}") String url) {
        this.client = builder.baseUrl(url).build();
    }
    public ResponseEntity<String> crear(String json, String authorization) {
        return client.post().uri("/api/inscripciones").headers(h -> bearer(h, authorization))
                .header(HttpHeaders.CONTENT_TYPE, "application/json").body(json)
                .retrieve().toEntity(String.class);
    }
    public ResponseEntity<String> obtener(Long id, String authorization) {
        return client.get().uri("/api/inscripciones/{id}", id).headers(h -> bearer(h, authorization))
                .retrieve().toEntity(String.class);
    }
    public ResponseEntity<String> consumir(String authorization) {
        return client.post().uri("/api/inscripciones/consumir-siguiente")
                .headers(h -> bearer(h, authorization)).retrieve().toEntity(String.class);
    }
    private void bearer(HttpHeaders headers, String authorization) {
        if (authorization != null && !authorization.isBlank()) headers.set(HttpHeaders.AUTHORIZATION, authorization);
    }
}
