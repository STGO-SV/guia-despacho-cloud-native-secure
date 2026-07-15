package com.duoc.eft.cursos.dto;

import com.duoc.eft.cursos.model.Curso;

public record CursoResponse(Long id, String titulo, String descripcion, String instructor,
                            String estado, String materialS3Key) {
    public static CursoResponse from(Curso curso) {
        return new CursoResponse(curso.getId(), curso.getTitulo(), curso.getDescripcion(),
                curso.getInstructor(), curso.getEstado(), curso.getMaterialS3Key());
    }
}

