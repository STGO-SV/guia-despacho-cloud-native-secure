package com.duoc.guia_despacho.dto;

import java.time.Instant;
import java.util.UUID;

public record ProcesamientoAceptadoResponse(
        UUID eventoId,
        Long guiaId,
        String estado,
        boolean errorSimulado,
        Instant aceptadoEn
) {
}
