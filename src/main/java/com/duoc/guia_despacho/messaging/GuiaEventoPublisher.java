package com.duoc.guia_despacho.messaging;

import com.duoc.guia_despacho.model.GuiaDespacho;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GuiaEventoPublisher {

    private static final String TIPO_EVENTO = "GUIA_CREADA";

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbit.exchange}")
    private String exchange;

    @Value("${app.rabbit.created-routing-key}")
    private String routingKey;

    public UUID publicar(GuiaDespacho guia, boolean simularError) {
        UUID eventoId = UUID.randomUUID();
        GuiaEvento evento = new GuiaEvento(
                eventoId,
                guia.getId(),
                Instant.now(),
                TIPO_EVENTO,
                guia.getNumeroGuia(),
                guia.getTransportista(),
                guia.getFecha(),
                guia.getDestinatario(),
                guia.getDireccionDestino(),
                guia.getDescripcionCarga(),
                guia.getEstado(),
                guia.getS3Key(),
                0,
                simularError
        );

        rabbitTemplate.convertAndSend(exchange, routingKey, evento, message -> {
            message.getMessageProperties().setMessageId(eventoId.toString());
            message.getMessageProperties().setCorrelationId(guia.getId().toString());
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setHeader("guiaId", guia.getId());
            return message;
        });
        log.info("Evento de guia publicado eventoId={} guiaId={} routingKey={}", eventoId, guia.getId(), routingKey);
        return eventoId;
    }
}
