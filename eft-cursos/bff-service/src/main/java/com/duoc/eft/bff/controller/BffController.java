package com.duoc.eft.bff.controller;

import com.duoc.eft.bff.client.CursosClient;
import com.duoc.eft.bff.client.InscripcionesClient;
import com.duoc.eft.bff.dto.InscripcionRequest;
import com.duoc.eft.bff.http.DownstreamResponseSanitizer;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bff")
public class BffController {
    private final CursosClient cursos;
    private final InscripcionesClient inscripciones;
    public BffController(CursosClient cursos, InscripcionesClient inscripciones) {
        this.cursos = cursos; this.inscripciones = inscripciones;
    }
    @GetMapping("/cursos")
    ResponseEntity<String> listarCursos(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return DownstreamResponseSanitizer.sanitize(cursos.listar(auth));
    }
    @PostMapping("/cursos")
    ResponseEntity<String> crearCurso(@RequestBody String json,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return DownstreamResponseSanitizer.sanitize(cursos.crear(json, auth));
    }
    @PostMapping("/inscripciones")
    ResponseEntity<String> crearInscripcion(@Valid @RequestBody InscripcionRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return DownstreamResponseSanitizer.sanitize(inscripciones.crear(request, auth));
    }
    @GetMapping("/inscripciones/{id}")
    ResponseEntity<String> obtenerInscripcion(@PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return DownstreamResponseSanitizer.sanitize(inscripciones.obtener(id, auth));
    }
    @PostMapping("/inscripciones/consumir")
    ResponseEntity<String> consumirInscripcion(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        return DownstreamResponseSanitizer.sanitize(inscripciones.consumir(auth));
    }
}
