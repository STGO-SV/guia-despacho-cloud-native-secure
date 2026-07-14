package com.duoc.guia_despacho.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.duoc.guia_despacho.model.GuiaDespacho;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GuiaEventoPublisherTests {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void publicaEventoConIdentificadoresYMensajePersistente() throws Exception {
        GuiaEventoPublisher publisher = new GuiaEventoPublisher(rabbitTemplate);
        ReflectionTestUtils.setField(publisher, "exchange", "guia.exchange");
        ReflectionTestUtils.setField(publisher, "routingKey", "guia.creada");
        GuiaDespacho guia = GuiaDespacho.builder()
                .id(9L).numeroGuia("GD-009").transportista("Transportista")
                .fecha(LocalDate.parse("2026-07-13")).destinatario("Cliente")
                .direccionDestino("Direccion").descripcionCarga("Carga").estado("CREADA").build();

        var eventoId = publisher.publicar(guia, false);

        ArgumentCaptor<GuiaEvento> eventCaptor = ArgumentCaptor.forClass(GuiaEvento.class);
        ArgumentCaptor<MessagePostProcessor> processorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq("guia.exchange"), eq("guia.creada"), eventCaptor.capture(), processorCaptor.capture());
        Message message = processorCaptor.getValue().postProcessMessage(new Message(new byte[0]));
        assertThat(eventCaptor.getValue().eventoId()).isEqualTo(eventoId);
        assertThat(eventCaptor.getValue().guiaId()).isEqualTo(9L);
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(eventoId.toString());
        assertThat(message.getMessageProperties().getCorrelationId()).isEqualTo("9");
        assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
    }
}
