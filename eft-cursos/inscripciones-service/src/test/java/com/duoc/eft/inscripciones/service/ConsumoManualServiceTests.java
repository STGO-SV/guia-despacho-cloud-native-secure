package com.duoc.eft.inscripciones.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.duoc.eft.inscripciones.messaging.InscripcionCreadaEvento;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class ConsumoManualServiceTests {
    @Mock RabbitTemplate template;
    @Mock InscripcionProcesamientoService procesamiento;
    @Test void consumeYProcesaEvento() {
        InscripcionCreadaEvento evento = new InscripcionCreadaEvento(UUID.randomUUID(), 1L, 2L, "e", Instant.now());
        when(template.receiveAndConvert("cola")).thenReturn(evento);
        assertTrue(new ConsumoManualService(template, procesamiento, "cola").consumirSiguiente().isPresent());
        verify(procesamiento).procesar(evento);
    }
    @Test void colaVaciaDevuelveOptionalVacio() {
        assertTrue(new ConsumoManualService(template, procesamiento, "cola").consumirSiguiente().isEmpty());
    }
}
