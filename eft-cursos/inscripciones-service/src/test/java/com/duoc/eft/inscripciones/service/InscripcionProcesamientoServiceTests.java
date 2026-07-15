package com.duoc.eft.inscripciones.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.duoc.eft.inscripciones.messaging.InscripcionCreadaEvento;
import com.duoc.eft.inscripciones.repository.InscripcionProcesadaRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InscripcionProcesamientoServiceTests {
    @Mock InscripcionProcesadaRepository repository;
    @Test void evitaEventoDuplicado() {
        UUID id = UUID.randomUUID();
        when(repository.existsByEventoId(id.toString())).thenReturn(true);
        new InscripcionProcesamientoService(repository).procesar(evento(id));
        verify(repository, never()).save(any());
    }
    @Test void persisteEventoNuevo() {
        UUID id = UUID.randomUUID();
        when(repository.existsByEventoId(id.toString())).thenReturn(false);
        new InscripcionProcesamientoService(repository).procesar(evento(id));
        verify(repository).save(any());
    }
    private InscripcionCreadaEvento evento(UUID id) {
        return new InscripcionCreadaEvento(id, 1L, 2L, "estudiante", Instant.now());
    }
}

