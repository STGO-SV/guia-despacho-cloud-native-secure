package com.duoc.eft.cursos.controller;

import com.duoc.eft.cursos.dto.CursoRequest;
import com.duoc.eft.cursos.dto.CursoResponse;
import com.duoc.eft.cursos.dto.MaterialResponse;
import com.duoc.eft.cursos.service.CursoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cursos")
public class CursoController {
    private final CursoService service;
    public CursoController(CursoService service) { this.service = service; }

    @PostMapping
    ResponseEntity<CursoResponse> crear(@Valid @RequestBody CursoRequest request) {
        CursoResponse response = service.crear(request);
        return ResponseEntity.created(URI.create("/api/cursos/" + response.id())).body(response);
    }
    @GetMapping List<CursoResponse> listar() { return service.listar(); }
    @GetMapping("/{id}") CursoResponse obtener(@PathVariable Long id) { return service.obtener(id); }
    @PutMapping("/{id}") CursoResponse actualizar(@PathVariable Long id, @Valid @RequestBody CursoRequest request) {
        return service.actualizar(id, request);
    }
    @DeleteMapping("/{id}") ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id); return ResponseEntity.noContent().build();
    }
    @PostMapping(path = "/{id}/material", consumes = "multipart/form-data")
    MaterialResponse material(@PathVariable Long id, @RequestPart("archivo") MultipartFile archivo) {
        return service.guardarMaterial(id, archivo);
    }
}

