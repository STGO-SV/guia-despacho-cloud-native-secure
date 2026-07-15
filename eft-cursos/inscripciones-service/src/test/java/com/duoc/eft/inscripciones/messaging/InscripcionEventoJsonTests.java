package com.duoc.eft.inscripciones.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InscripcionEventoJsonTests {
    @Test void esSerializable() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        InscripcionCreadaEvento evento = new InscripcionCreadaEvento(UUID.randomUUID(), 1L, 2L, "e", Instant.now());
        assertEquals(evento.eventoId(), mapper.readValue(mapper.writeValueAsBytes(evento), InscripcionCreadaEvento.class).eventoId());
    }
}

