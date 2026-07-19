package com.duoc.eft.inscripciones.dto;

import com.duoc.eft.inscripciones.model.Inscripcion;
import java.time.LocalDate;
import java.util.UUID;

public record InscripcionResponse(Long id, Long cursoId, String estudianteId,
                                  LocalDate fechaInscripcion, String estado, UUID eventoId,
                                  String comprobanteS3Key, boolean comprobanteAlmacenado) {
    public static InscripcionResponse from(Inscripcion i) {
        return new InscripcionResponse(i.getId(), i.getCursoId(), i.getEstudianteId(),
                i.getFechaInscripcion(), i.getEstado(), null,
                i.getComprobanteS3Key(), i.isComprobanteAlmacenado());
    }
    public static InscripcionResponse from(Inscripcion i, UUID eventoId) {
        return new InscripcionResponse(i.getId(), i.getCursoId(), i.getEstudianteId(),
                i.getFechaInscripcion(), i.getEstado(), eventoId,
                i.getComprobanteS3Key(), i.isComprobanteAlmacenado());
    }
}
