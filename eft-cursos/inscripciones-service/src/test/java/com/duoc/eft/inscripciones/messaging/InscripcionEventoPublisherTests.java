package com.duoc.eft.inscripciones.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class InscripcionEventoPublisherTests {
    @Mock RabbitTemplate template;
    @Test void publicaEnExchangeYRoutingKey() {
        InscripcionCreadaEvento evento = new InscripcionCreadaEvento(UUID.randomUUID(), 1L, 2L, "e", Instant.now());
        new InscripcionEventoPublisher(template, "cursos.exchange", "inscripcion.creada").publicar(evento);
        verify(template).convertAndSend(eq("cursos.exchange"), eq("inscripcion.creada"), eq(evento),
                any(MessagePostProcessor.class));
    }
}
