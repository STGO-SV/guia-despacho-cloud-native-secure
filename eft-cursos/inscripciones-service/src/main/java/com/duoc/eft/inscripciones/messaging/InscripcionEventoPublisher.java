package com.duoc.eft.inscripciones.messaging;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InscripcionEventoPublisher {
    private final RabbitTemplate template;
    private final String exchange;
    private final String routingKey;

    public InscripcionEventoPublisher(RabbitTemplate template,
            @Value("${app.rabbit.exchange}") String exchange,
            @Value("${app.rabbit.created-routing-key}") String routingKey) {
        this.template = template; this.exchange = exchange; this.routingKey = routingKey;
    }

    public void publicar(InscripcionCreadaEvento evento) {
        template.convertAndSend(exchange, routingKey, evento, message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setMessageId(evento.eventoId().toString());
            return message;
        });
    }
}

