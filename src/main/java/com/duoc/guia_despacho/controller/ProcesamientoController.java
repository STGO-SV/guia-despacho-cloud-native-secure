package com.duoc.guia_despacho.controller;

import com.duoc.guia_despacho.dto.EstadoColasResponse;
import com.duoc.guia_despacho.dto.ProcesamientoAceptadoResponse;
import com.duoc.guia_despacho.service.EstadoColasService;
import com.duoc.guia_despacho.service.GuiaDespachoService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/procesamiento")
@RequiredArgsConstructor
public class ProcesamientoController {

    private final GuiaDespachoService guiaService;
    private final EstadoColasService estadoColasService;

    @PostMapping("/guias/{id}")
    public ResponseEntity<ProcesamientoAceptadoResponse> publicar(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean simularError
    ) {
        UUID eventoId = guiaService.publicarProcesamiento(id, simularError);
        return ResponseEntity.accepted().body(new ProcesamientoAceptadoResponse(
                eventoId,
                id,
                "ACEPTADO",
                simularError,
                Instant.now()
        ));
    }

    @GetMapping("/colas")
    public EstadoColasResponse estadoColas() {
        return estadoColasService.obtenerEstado();
    }
}
