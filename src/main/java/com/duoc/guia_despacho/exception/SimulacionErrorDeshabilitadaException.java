package com.duoc.guia_despacho.exception;

public class SimulacionErrorDeshabilitadaException extends RuntimeException {

    public SimulacionErrorDeshabilitadaException() {
        super("La simulacion controlada de errores RabbitMQ esta deshabilitada");
    }
}
