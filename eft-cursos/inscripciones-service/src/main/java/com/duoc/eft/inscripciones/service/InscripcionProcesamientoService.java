package com.duoc.eft.inscripciones.service;

import com.duoc.eft.inscripciones.messaging.InscripcionCreadaEvento;
import com.duoc.eft.inscripciones.model.InscripcionProcesada;
import com.duoc.eft.inscripciones.repository.InscripcionProcesadaRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InscripcionProcesamientoService {
    private static final Logger LOG = LoggerFactory.getLogger(InscripcionProcesamientoService.class);
    private final InscripcionProcesadaRepository repository;
    public InscripcionProcesamientoService(InscripcionProcesadaRepository repository) { this.repository = repository; }

    @Transactional
    public void procesar(InscripcionCreadaEvento evento) {
        if (evento.simularError()) {
            LOG.warn("Fallo academico controlado para evento {}", evento.eventoId());
            throw new IllegalStateException("Fallo academico controlado");
        }
        String eventoId = evento.eventoId().toString();
        if (repository.existsByEventoId(eventoId)) return;
        InscripcionProcesada procesada = new InscripcionProcesada();
        procesada.setEventoId(eventoId);
        procesada.setInscripcionId(evento.inscripcionId());
        procesada.setFechaProcesamiento(Instant.now());
        repository.save(procesada);
    }

    @Transactional(readOnly = true)
    public long cantidadProcesada(String eventoId) { return repository.countByEventoId(eventoId); }
}
