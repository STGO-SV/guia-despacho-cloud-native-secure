package com.duoc.guia_despacho.service;

import com.duoc.guia_despacho.messaging.GuiaEvento;
import com.duoc.guia_despacho.model.GuiaProcesada;
import com.duoc.guia_despacho.repository.GuiaProcesadaRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuiaProcesamientoService {

    private final GuiaProcesadaRepository repository;

    @Transactional
    public void procesar(GuiaEvento evento) {
        if (evento.simularError()) {
            throw new IllegalStateException("Fallo controlado solicitado para demostrar la cola de errores");
        }

        if (repository.existsByEventoId(evento.eventoId().toString())) {
            log.info("Evento duplicado ignorado eventoId={} guiaId={}", evento.eventoId(), evento.guiaId());
            return;
        }

        repository.save(GuiaProcesada.builder()
                .eventoId(evento.eventoId().toString())
                .guiaId(evento.guiaId())
                .tipoEvento(evento.tipoEvento())
                .numeroGuia(evento.numeroGuia())
                .transportista(evento.transportista())
                .fechaGuia(evento.fechaGuia())
                .destinatario(evento.destinatario())
                .direccionDestino(evento.direccionDestino())
                .descripcionCarga(evento.descripcionCarga())
                .estado(evento.estado())
                .s3Key(evento.s3Key())
                .fechaEvento(evento.fechaEvento())
                .fechaProcesamiento(Instant.now())
                .build());
        log.info("Evento persistido eventoId={} guiaId={}", evento.eventoId(), evento.guiaId());
    }
}
