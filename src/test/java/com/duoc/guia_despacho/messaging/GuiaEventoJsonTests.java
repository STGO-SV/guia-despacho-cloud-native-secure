package com.duoc.guia_despacho.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GuiaEventoJsonTests {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void eventoSeSerializaYDeserializaComoJsonSinEntidadJpa() throws Exception {
        GuiaEvento original = new GuiaEvento(
                UUID.randomUUID(), 7L, Instant.parse("2026-07-13T12:00:00Z"), "GUIA_CREADA", "GD-007",
                "Transportes Norte", LocalDate.parse("2026-07-13"), "Cliente", "Direccion", "Carga",
                "CREADA", "guias/transportes-norte/2026/07/guia-7.pdf", 0, false
        );

        String json = objectMapper.writeValueAsString(original);
        GuiaEvento restored = objectMapper.readValue(json, GuiaEvento.class);

        assertThat(restored).isEqualTo(original);
        assertThat(json).contains("eventoId", "guiaId", "fechaEvento", "tipoEvento");
    }
}
