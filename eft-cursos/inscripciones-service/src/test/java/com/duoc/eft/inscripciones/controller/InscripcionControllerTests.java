package com.duoc.eft.inscripciones.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.duoc.eft.inscripciones.config.SecurityConfig;
import com.duoc.eft.inscripciones.dto.InscripcionRequest;
import com.duoc.eft.inscripciones.dto.InscripcionResponse;
import com.duoc.eft.inscripciones.exception.InscripcionDuplicadaException;
import com.duoc.eft.inscripciones.exception.ComprobanteStorageException;
import com.duoc.eft.inscripciones.service.ConsumoManualService;
import com.duoc.eft.inscripciones.service.InscripcionProcesamientoService;
import com.duoc.eft.inscripciones.service.InscripcionService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InscripcionController.class)
@Import(SecurityConfig.class)
class InscripcionControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean InscripcionService service;
    @MockitoBean ConsumoManualService consumoManual;
    @MockitoBean InscripcionProcesamientoService procesamiento;
    @MockitoBean JwtDecoder jwtDecoder;

    @BeforeEach void tokenEstudiante() {
        Jwt token = Jwt.withTokenValue("token-estudiante").header("alg", "none")
                .subject("sub-estudiante-autenticado").audience(List.of("api"))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                .claim("extension_RolGuia", "ESTUDIANTE").build();
        when(jwtDecoder.decode("token-estudiante")).thenReturn(token);
        when(service.crear(new InscripcionRequest(3L, false), "sub-estudiante-autenticado"))
                .thenReturn(new InscripcionResponse(9L, 3L, "sub-estudiante-autenticado",
                        LocalDate.now(), "CREADA", UUID.randomUUID(),
                        "2026/inscripciones/9/comprobante-inscripcion.pdf", true));
    }

    @Test void requestPublicoSoloRequiereCursoYUsaSub() throws Exception {
        mvc.perform(post("/api/inscripciones")
                        .header("Authorization", "Bearer token-estudiante")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cursoId\":3}"))
                .andExpect(status().isCreated());
        verify(service).crear(new InscripcionRequest(3L, false), "sub-estudiante-autenticado");
    }

    @Test void identidadDelBodyNoPuedeReemplazarSub() throws Exception {
        mvc.perform(post("/api/inscripciones")
                        .header("Authorization", "Bearer token-estudiante")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cursoId\":3,\"estudianteId\":\"sub-de-otra-persona\"}"))
                .andExpect(status().isCreated());
        verify(service).crear(new InscripcionRequest(3L, false), "sub-estudiante-autenticado");
    }

    @Test void segundaInscripcionDelMismoSubYCursoDevuelve409() throws Exception {
        when(service.crear(new InscripcionRequest(3L, false), "sub-estudiante-autenticado"))
                .thenThrow(new InscripcionDuplicadaException());

        mvc.perform(post("/api/inscripciones")
                        .header("Authorization", "Bearer token-estudiante")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cursoId\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("El estudiante ya está inscrito en este curso"));
    }

    @Test void instructorRecibe403YNoIniciaInscripcion() throws Exception {
        mvc.perform(post("/api/inscripciones")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cursoId\":3}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void falloS3Devuelve502Controlado() throws Exception {
        when(service.crear(new InscripcionRequest(3L, false), "sub-estudiante-autenticado"))
                .thenThrow(new ComprobanteStorageException("detalle interno de AWS"));

        mvc.perform(post("/api/inscripciones")
                        .header("Authorization", "Bearer token-estudiante")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cursoId\":3}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message")
                        .value("No fue posible almacenar el comprobante de inscripción"));
    }
}
