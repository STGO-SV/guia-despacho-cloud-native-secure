package com.duoc.eft.inscripciones.controller;

import com.duoc.eft.inscripciones.dto.InscripcionRequest;
import com.duoc.eft.inscripciones.dto.InscripcionResponse;
import com.duoc.eft.inscripciones.messaging.InscripcionCreadaEvento;
import com.duoc.eft.inscripciones.service.ConsumoManualService;
import com.duoc.eft.inscripciones.service.InscripcionService;
import com.duoc.eft.inscripciones.service.InscripcionProcesamientoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inscripciones")
public class InscripcionController {
    private final InscripcionService service;
    private final ConsumoManualService consumoManual;
    private final InscripcionProcesamientoService procesamiento;
    public InscripcionController(InscripcionService service, ConsumoManualService consumoManual,
            InscripcionProcesamientoService procesamiento) {
        this.service = service; this.consumoManual = consumoManual; this.procesamiento = procesamiento;
    }
    @PostMapping ResponseEntity<InscripcionResponse> crear(@Valid @RequestBody InscripcionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        InscripcionResponse response = service.crear(request, jwt.getSubject());
        return ResponseEntity.created(URI.create("/api/inscripciones/" + response.id())).body(response);
    }
    @GetMapping List<InscripcionResponse> listar() { return service.listar(); }
    @GetMapping("/{id}") InscripcionResponse obtener(@PathVariable Long id) { return service.obtener(id); }
    @GetMapping("/curso/{cursoId}") List<InscripcionResponse> porCurso(@PathVariable Long cursoId) {
        return service.porCurso(cursoId);
    }
    @PostMapping("/consumir-siguiente") ResponseEntity<InscripcionCreadaEvento> consumirSiguiente() {
        return consumoManual.consumirSiguiente().map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
    @PostMapping("/republicar-evento") ResponseEntity<Void> republicar(
            @RequestBody InscripcionCreadaEvento evento) {
        service.republicar(evento);
        return ResponseEntity.accepted().build();
    }
    @GetMapping("/procesadas/{eventoId}") Map<String, Object> procesadas(@PathVariable UUID eventoId) {
        long cantidad = procesamiento.cantidadProcesada(eventoId.toString());
        return Map.of("eventoId", eventoId, "cantidad", cantidad, "idempotente", cantidad <= 1);
    }
}
