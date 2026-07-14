package com.duoc.guia_despacho.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;

class RabbitMqConfigTests {

    private final RabbitMqConfig config = new RabbitMqConfig();

    @Test
    void colaPrincipalEsDurableYTieneDeadLetterRouting() {
        Queue queue = config.guiaProcesamientoQueue(
                "guia.procesamiento.queue", "guia.exchange", "guia.error");

        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", "guia.exchange")
                .containsEntry("x-dead-letter-routing-key", "guia.error");
    }

    @Test
    void colaErrorEsDurable() {
        assertThat(config.guiaErrorQueue("guia.error.queue").isDurable()).isTrue();
    }
}
