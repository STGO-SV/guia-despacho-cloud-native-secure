package com.duoc.eft.inscripciones.dto;

import com.duoc.eft.inscripciones.model.Inscripcion;
import java.time.LocalDate;

public record InscripcionResponse(Long id, Long cursoId, String estudianteId,
                                  LocalDate fechaInscripcion, String estado) {
    public static InscripcionResponse from(Inscripcion i) {
        return new InscripcionResponse(i.getId(), i.getCursoId(), i.getEstudianteId(),
                i.getFechaInscripcion(), i.getEstado());
    }
}

