package com.duoc.eft.inscripciones.messaging;

import com.duoc.eft.inscripciones.service.InscripcionProcesamientoService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class InscripcionEventoConsumer {
    private final InscripcionProcesamientoService service;
    public InscripcionEventoConsumer(InscripcionProcesamientoService service) { this.service = service; }
    @RabbitListener(queues = "${app.rabbit.main-queue}")
    public void consumir(InscripcionCreadaEvento evento) { service.procesar(evento); }
}

