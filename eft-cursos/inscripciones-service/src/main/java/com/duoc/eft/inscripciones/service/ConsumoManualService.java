package com.duoc.eft.inscripciones.service;

import com.duoc.eft.inscripciones.messaging.InscripcionCreadaEvento;
import java.util.Optional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConsumoManualService {
    private final RabbitTemplate template;
    private final InscripcionProcesamientoService procesamiento;
    private final String queue;

    public ConsumoManualService(RabbitTemplate template, InscripcionProcesamientoService procesamiento,
            @Value("${app.rabbit.main-queue}") String queue) {
        this.template = template; this.procesamiento = procesamiento; this.queue = queue;
    }

    public Optional<InscripcionCreadaEvento> consumirSiguiente() {
        Object message = template.receiveAndConvert(queue);
        if (!(message instanceof InscripcionCreadaEvento evento)) return Optional.empty();
        procesamiento.procesar(evento);
        return Optional.of(evento);
    }
}
