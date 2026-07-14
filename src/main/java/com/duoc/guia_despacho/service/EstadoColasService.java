package com.duoc.guia_despacho.service;

import com.duoc.guia_despacho.dto.EstadoColasResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EstadoColasService {

    private final AmqpAdmin amqpAdmin;

    @Value("${app.rabbit.main-queue}")
    private String mainQueue;

    @Value("${app.rabbit.error-queue}")
    private String errorQueue;

    public EstadoColasResponse obtenerEstado() {
        QueueInformation main = requireQueue(mainQueue);
        QueueInformation error = requireQueue(errorQueue);
        return new EstadoColasResponse(
                mainQueue,
                main.getMessageCount(),
                main.getConsumerCount(),
                errorQueue,
                error.getMessageCount(),
                error.getConsumerCount()
        );
    }

    private QueueInformation requireQueue(String name) {
        QueueInformation information = amqpAdmin.getQueueInfo(name);
        if (information == null) {
            throw new IllegalStateException("RabbitMQ no reporta la cola durable " + name);
        }
        return information;
    }
}
