package com.duoc.eft.inscripciones.messaging;

import static org.mockito.Mockito.verify;

import com.duoc.eft.inscripciones.service.InscripcionProcesamientoService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InscripcionEventoConsumerTests {
    @Mock InscripcionProcesamientoService service;
    @Test void delegaProcesamiento() {
        InscripcionCreadaEvento evento = new InscripcionCreadaEvento(UUID.randomUUID(), 1L, 2L, "e", Instant.now());
        new InscripcionEventoConsumer(service).consumir(evento);
        verify(service).procesar(evento);
    }
}

