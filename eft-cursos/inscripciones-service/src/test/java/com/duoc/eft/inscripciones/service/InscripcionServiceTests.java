package com.duoc.eft.inscripciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.duoc.eft.inscripciones.dto.InscripcionRequest;
import com.duoc.eft.inscripciones.messaging.InscripcionEventoPublisher;
import com.duoc.eft.inscripciones.model.Inscripcion;
import com.duoc.eft.inscripciones.repository.InscripcionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InscripcionServiceTests {
    @Mock InscripcionRepository repository;
    @Mock InscripcionEventoPublisher publisher;
    @Test void crearPersisteYPublica() {
        when(repository.save(any())).thenAnswer(inv -> { Inscripcion i = inv.getArgument(0); i.setId(7L); return i; });
        InscripcionService service = new InscripcionService(repository, publisher);
        assertEquals(7L, service.crear(new InscripcionRequest(3L, "estudiante-1", false)).id());
        verify(publisher).publicar(any());
    }
}
