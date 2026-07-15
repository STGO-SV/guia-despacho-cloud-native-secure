package com.duoc.eft.cursos.exception;

public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(Long id) {
        super("Curso no encontrado: " + id);
    }
}

