package com.duoc.eft.inscripciones.exception;

public class ComprobanteGeneracionException extends RuntimeException {
    public ComprobanteGeneracionException(Throwable cause) {
        super("No fue posible generar el comprobante de inscripción", cause);
    }
}
