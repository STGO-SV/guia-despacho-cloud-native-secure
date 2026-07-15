package com.duoc.eft.inscripciones.exception;

public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(Long id) { super("Inscripcion no encontrada: " + id); }
}

