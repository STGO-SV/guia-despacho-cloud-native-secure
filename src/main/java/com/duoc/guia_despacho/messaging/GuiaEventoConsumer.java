package com.duoc.guia_despacho.messaging;

import com.duoc.guia_despacho.service.GuiaProcesamientoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GuiaEventoConsumer {

    private final GuiaProcesamientoService procesamientoService;

    @RabbitListener(queues = "${app.rabbit.main-queue}", id = "guiaProcesamientoListener")
    public void consumir(GuiaEvento evento) {
        log.info("Consumiendo eventoId={} guiaId={} intento={}", evento.eventoId(), evento.guiaId(), evento.intento());
        procesamientoService.procesar(evento);
    }
}
