package com.duoc.eft.inscripciones.service;

import com.duoc.eft.inscripciones.messaging.InscripcionCreadaEvento;
import com.duoc.eft.inscripciones.model.InscripcionProcesada;
import com.duoc.eft.inscripciones.repository.InscripcionProcesadaRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InscripcionProcesamientoService {
    private final InscripcionProcesadaRepository repository;
    public InscripcionProcesamientoService(InscripcionProcesadaRepository repository) { this.repository = repository; }

    @Transactional
    public void procesar(InscripcionCreadaEvento evento) {
        String eventoId = evento.eventoId().toString();
        if (repository.existsByEventoId(eventoId)) return;
        InscripcionProcesada procesada = new InscripcionProcesada();
        procesada.setEventoId(eventoId);
        procesada.setInscripcionId(evento.inscripcionId());
        procesada.setFechaProcesamiento(Instant.now());
        repository.save(procesada);
    }
}

