package com.duoc.eft.bff.exception;

import com.duoc.eft.bff.http.DownstreamResponseSanitizer;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

@RestControllerAdvice
public class BffExceptionHandler {
    @ExceptionHandler(RestClientResponseException.class)
    ResponseEntity<String> downstream(RestClientResponseException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .headers(DownstreamResponseSanitizer.copyAllowedHeaders(ex.getResponseHeaders()))
                .body(ex.getResponseBodyAsString());
    }
    @ExceptionHandler(ResourceAccessException.class)
    ResponseEntity<Map<String, Object>> unavailable(ResourceAccessException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "timestamp", Instant.now(), "status", 503, "message", "Servicio interno no disponible"));
    }
}
