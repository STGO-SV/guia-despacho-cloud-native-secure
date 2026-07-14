package com.duoc.guia_despacho.messaging;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GuiaEvento(
        UUID eventoId,
        Long guiaId,
        Instant fechaEvento,
        String tipoEvento,
        String numeroGuia,
        String transportista,
        LocalDate fechaGuia,
        String destinatario,
        String direccionDestino,
        String descripcionCarga,
        String estado,
        String s3Key,
        int intento,
        boolean simularError
) {
}
