package com.duoc.eft.inscripciones.service;

import com.duoc.eft.inscripciones.dto.InscripcionRequest;
import com.duoc.eft.inscripciones.dto.InscripcionResponse;
import com.duoc.eft.inscripciones.exception.RecursoNoEncontradoException;
import com.duoc.eft.inscripciones.exception.InscripcionDuplicadaException;
import com.duoc.eft.inscripciones.messaging.InscripcionCreadaEvento;
import com.duoc.eft.inscripciones.messaging.InscripcionEventoPublisher;
import com.duoc.eft.inscripciones.model.Inscripcion;
import com.duoc.eft.inscripciones.repository.InscripcionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InscripcionService {
    private final InscripcionRepository repository;
    private final InscripcionEventoPublisher publisher;
    public InscripcionService(InscripcionRepository repository, InscripcionEventoPublisher publisher) {
        this.repository = repository; this.publisher = publisher;
    }
    @Transactional
    public InscripcionResponse crear(InscripcionRequest request, String authenticatedSubject) {
        if (authenticatedSubject == null || authenticatedSubject.isBlank()) {
            throw new IllegalArgumentException("El token autenticado no contiene sub");
        }
        if (repository.existsByEstudianteIdAndCursoId(authenticatedSubject, request.cursoId())) {
            throw new InscripcionDuplicadaException();
        }
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setCursoId(request.cursoId()); inscripcion.setEstudianteId(authenticatedSubject);
        inscripcion.setFechaInscripcion(LocalDate.now()); inscripcion.setEstado("CREADA");
        try {
            inscripcion = repository.saveAndFlush(inscripcion);
        } catch (DataIntegrityViolationException ex) {
            throw new InscripcionDuplicadaException(ex);
        }
        UUID eventoId = UUID.randomUUID();
        publisher.publicar(new InscripcionCreadaEvento(eventoId, inscripcion.getId(),
                inscripcion.getCursoId(), inscripcion.getEstudianteId(), Instant.now(),
                request.simularError()));
        return InscripcionResponse.from(inscripcion, eventoId);
    }
    @Transactional(readOnly = true)
    public List<InscripcionResponse> listar() { return repository.findAll().stream().map(InscripcionResponse::from).toList(); }
    @Transactional(readOnly = true)
    public InscripcionResponse obtener(Long id) { return InscripcionResponse.from(repository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(id))); }
    @Transactional(readOnly = true)
    public List<InscripcionResponse> porCurso(Long cursoId) {
        return repository.findByCursoId(cursoId).stream().map(InscripcionResponse::from).toList();
    }

    public void republicar(InscripcionCreadaEvento evento) { publisher.publicar(evento); }
}
