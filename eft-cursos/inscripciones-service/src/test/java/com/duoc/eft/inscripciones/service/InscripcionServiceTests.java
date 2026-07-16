package com.duoc.eft.inscripciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.duoc.eft.inscripciones.dto.InscripcionRequest;
import com.duoc.eft.inscripciones.exception.InscripcionDuplicadaException;
import com.duoc.eft.inscripciones.messaging.InscripcionEventoPublisher;
import com.duoc.eft.inscripciones.messaging.InscripcionCreadaEvento;
import com.duoc.eft.inscripciones.model.Inscripcion;
import com.duoc.eft.inscripciones.repository.InscripcionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class InscripcionServiceTests {
    @Mock InscripcionRepository repository;
    @Mock InscripcionEventoPublisher publisher;
    @Test void crearPersisteSubAutenticadoYPublica() {
        when(repository.saveAndFlush(any())).thenAnswer(inv -> { Inscripcion i = inv.getArgument(0); i.setId(7L); return i; });
        InscripcionService service = new InscripcionService(repository, publisher);
        assertEquals(7L, service.crear(new InscripcionRequest(3L, false), "sub-estudiante-1").id());

        ArgumentCaptor<Inscripcion> saved = ArgumentCaptor.forClass(Inscripcion.class);
        verify(repository).saveAndFlush(saved.capture());
        assertEquals("sub-estudiante-1", saved.getValue().getEstudianteId());

        ArgumentCaptor<InscripcionCreadaEvento> published = ArgumentCaptor.forClass(InscripcionCreadaEvento.class);
        verify(publisher).publicar(published.capture());
        assertEquals("sub-estudiante-1", published.getValue().estudianteId());
    }

    @Test void rechazaTokenSinSub() {
        InscripcionService service = new InscripcionService(repository, publisher);
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(new InscripcionRequest(3L, false), " "));
    }

    @Test void duplicadoDetectadoNoPersisteNiPublica() {
        when(repository.existsByEstudianteIdAndCursoId("sub-a", 1L)).thenReturn(true);
        InscripcionService service = new InscripcionService(repository, publisher);

        assertThrows(InscripcionDuplicadaException.class,
                () -> service.crear(new InscripcionRequest(1L, false), "sub-a"));

        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(publisher);
    }

    @Test void carreraDeRestriccionUnicaDevuelveConflictoSinPublicar() {
        when(repository.existsByEstudianteIdAndCursoId("sub-a", 1L)).thenReturn(false);
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique"));
        InscripcionService service = new InscripcionService(repository, publisher);

        assertThrows(InscripcionDuplicadaException.class,
                () -> service.crear(new InscripcionRequest(1L, false), "sub-a"));

        verifyNoInteractions(publisher);
    }
}
