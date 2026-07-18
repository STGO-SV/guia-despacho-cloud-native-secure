package com.duoc.eft.bff.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

public final class DownstreamResponseSanitizer {
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "content-length");

    private DownstreamResponseSanitizer() {
    }

    public static ResponseEntity<String> sanitize(ResponseEntity<String> response) {
        return ResponseEntity.status(response.getStatusCode())
                .headers(copyAllowedHeaders(response.getHeaders()))
                .body(response.getBody());
    }

    public static HttpHeaders copyAllowedHeaders(HttpHeaders source) {
        HttpHeaders target = new HttpHeaders();
        if (source == null || source.isEmpty()) {
            return target;
        }

        Set<String> excluded = new HashSet<>(HOP_BY_HOP_HEADERS);
        source.getOrEmpty(HttpHeaders.CONNECTION).stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .forEach(excluded::add);

        source.forEach((name, values) -> {
            if (!excluded.contains(name.toLowerCase(Locale.ROOT))) {
                target.put(name, new ArrayList<>(values));
            }
        });
        return target;
    }
}
