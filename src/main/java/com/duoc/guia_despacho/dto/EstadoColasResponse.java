package com.duoc.guia_despacho.dto;

public record EstadoColasResponse(
        String colaPrincipal,
        int mensajesPrincipal,
        int consumidoresPrincipal,
        String colaErrores,
        int mensajesError,
        int consumidoresError
) {
}
