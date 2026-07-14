package com.duoc.guia_despacho.controller;

import com.duoc.guia_despacho.dto.ActualizarGuiaRequest;
import com.duoc.guia_despacho.dto.CrearGuiaRequest;
import com.duoc.guia_despacho.dto.GuiaDespachoResponse;
import com.duoc.guia_despacho.service.GuiaDespachoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guias")
@RequiredArgsConstructor
public class GuiaDespachoController {

    private final GuiaDespachoService guiaService;

    @PostMapping
    public ResponseEntity<GuiaDespachoResponse> crearGuia(@Valid @RequestBody CrearGuiaRequest request) {
        GuiaDespachoResponse creada = guiaService.crearGuia(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(creada.id())
                        .toUri())
                .body(creada);
    }

    @PostMapping("/{id}/subir-s3")
    public ResponseEntity<GuiaDespachoResponse> subirGuiaAS3(@PathVariable Long id) {
        return ResponseEntity.ok(guiaService.subirGuiaAS3(id));
    }

    @GetMapping("/{id}")
    public GuiaDespachoResponse obtenerGuia(@PathVariable Long id) {
        return guiaService.obtenerGuia(id);
    }

    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargarGuia(
            @PathVariable Long id,
            @RequestParam String transportista
    ) {
        byte[] pdf = guiaService.descargarGuia(id, transportista);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("guia-" + id + ".pdf")
                                .build()
                                .toString())
                .body(pdf);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GuiaDespachoResponse> actualizarGuia(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarGuiaRequest request
    ) {
        return ResponseEntity.ok(guiaService.actualizarGuia(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarGuia(@PathVariable Long id) {
        guiaService.eliminarGuia(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<GuiaDespachoResponse>> buscarGuias(
            @RequestParam String transportista,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return ResponseEntity.ok(guiaService.buscarPorTransportistaYFecha(transportista, fecha));
    }
}
