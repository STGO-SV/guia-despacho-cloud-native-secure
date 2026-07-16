package com.duoc.eft.inscripciones.exception;

public class InscripcionDuplicadaException extends RuntimeException {
    public InscripcionDuplicadaException() {
        super("El estudiante ya está inscrito en este curso");
    }

    public InscripcionDuplicadaException(Throwable cause) {
        super("El estudiante ya está inscrito en este curso", cause);
    }
}
