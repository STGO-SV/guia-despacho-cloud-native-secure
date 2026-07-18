package com.duoc.eft.bff.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class DownstreamResponseSanitizerTests {
    @Test
    void noReenviaTransferEncoding() {
        HttpHeaders downstreamHeaders = new HttpHeaders();
        downstreamHeaders.add(HttpHeaders.TRANSFER_ENCODING, "chunked");

        ResponseEntity<String> result = DownstreamResponseSanitizer.sanitize(
                ResponseEntity.ok().headers(downstreamHeaders).body("{}"));

        assertThat(headerNames(result)).doesNotContain("transfer-encoding");
    }

    @Test
    void comparaEncabezadosSinDistinguirMayusculasYMinusculas() {
        HttpHeaders downstreamHeaders = new HttpHeaders();
        downstreamHeaders.add("tRaNsFeR-EnCoDiNg", "chunked");
        downstreamHeaders.add("cOnTeNt-LeNgTh", "2");
        downstreamHeaders.add("CoNnEcTiOn", "X-Internal");
        downstreamHeaders.add("X-Internal", "no reenviar");

        ResponseEntity<String> result = DownstreamResponseSanitizer.sanitize(
                ResponseEntity.ok().headers(downstreamHeaders).body("{}"));

        assertThat(headerNames(result)).doesNotContain(
                "transfer-encoding", "content-length", "connection", "x-internal");
    }

    @Test
    void conservaContentType() {
        HttpHeaders downstreamHeaders = new HttpHeaders();
        downstreamHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> result = DownstreamResponseSanitizer.sanitize(
                ResponseEntity.ok().headers(downstreamHeaders).body("{}"));

        assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    private static Iterable<String> headerNames(ResponseEntity<String> response) {
        return response.getHeaders().keySet().stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .toList();
    }
}
