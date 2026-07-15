package com.duoc.eft.inscripciones.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;

class RabbitMqConfigTests {
    @Test void colaPrincipalEsDurableYTieneDlq() {
        Queue queue = new RabbitMqConfig().inscripcionesQueue(
                "inscripciones.procesamiento.queue", "cursos.exchange", "inscripcion.error");
        assertTrue(queue.isDurable());
        assertEquals("cursos.exchange", queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("inscripcion.error", queue.getArguments().get("x-dead-letter-routing-key"));
    }
}

