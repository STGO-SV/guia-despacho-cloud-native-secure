package com.duoc.eft.bff.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duoc.eft.bff.client.CursosClient;
import com.duoc.eft.bff.client.InscripcionesClient;
import com.duoc.eft.bff.config.SecurityConfig;
import com.duoc.eft.bff.dto.InscripcionRequest;
import java.net.ConnectException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

@WebMvcTest(BffController.class)
@Import(SecurityConfig.class)
class BffControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean CursosClient cursos;
    @MockitoBean InscripcionesClient inscripciones;
    @MockitoBean JwtDecoder jwtDecoder;

    @BeforeEach void tokenAcademico() {
        Jwt token = Jwt.withTokenValue("token-demo").header("alg", "none")
                .subject("estudiante-1").audience(List.of("api"))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                .claim("extension_RolGuia", "ESTUDIANTE").build();
        when(jwtDecoder.decode("token-demo")).thenReturn(token);
    }

    @Test void reenviaConsultaYAuthorization() throws Exception {
        when(cursos.listar("Bearer token-demo")).thenReturn(ResponseEntity.ok("[]"));
        mvc.perform(get("/api/bff/cursos").header("Authorization", "Bearer token-demo"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(cursos).listar("Bearer token-demo");
    }
    @Test void reenviaCreacionInscripcion() throws Exception {
        String json = "{\"cursoId\":1}";
        InscripcionRequest request = new InscripcionRequest(1L, false);
        when(inscripciones.crear(request, "Bearer token-demo"))
                .thenReturn(ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(json));
        mvc.perform(post("/api/bff/inscripciones").header("Authorization", "Bearer token-demo")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        verify(inscripciones).crear(request, "Bearer token-demo");
    }
    @Test void noReenviaIdentidadControladaPorElNavegador() throws Exception {
        String json = "{\"cursoId\":1,\"estudianteId\":\"otra-persona\"}";
        InscripcionRequest sanitized = new InscripcionRequest(1L, false);
        when(inscripciones.crear(sanitized, "Bearer token-demo"))
                .thenReturn(ResponseEntity.status(201).body("{}"));
        mvc.perform(post("/api/bff/inscripciones").header("Authorization", "Bearer token-demo")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        verify(inscripciones).crear(sanitized, "Bearer token-demo");
    }
    @Test void propaga409DeInscripcionDuplicada() throws Exception {
        InscripcionRequest request = new InscripcionRequest(1L, false);
        when(inscripciones.crear(request, "Bearer token-demo"))
                .thenReturn(ResponseEntity.status(409).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":409,\"message\":\"El estudiante ya está inscrito en este curso\"}"));

        mvc.perform(post("/api/bff/inscripciones").header("Authorization", "Bearer token-demo")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cursoId\":1}"))
                .andExpect(status().isConflict())
                .andExpect(content().json("{\"status\":409,\"message\":\"El estudiante ya está inscrito en este curso\"}"));
    }
    @Test void instructorNoPuedeUsarFlujoDeInscripcion() throws Exception {
        mvc.perform(post("/api/bff/inscripciones")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cursoId\":1}"))
                .andExpect(status().isForbidden());
    }
    @Test void manejaServicioNoDisponible() throws Exception {
        when(cursos.listar(null)).thenThrow(new ResourceAccessException("sin conexion", new ConnectException()));
        mvc.perform(get("/api/bff/cursos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"))))
                .andExpect(status().isServiceUnavailable());
    }
    @Test void corsAceptaSoloFrontendLocalConfigurado() throws Exception {
        mvc.perform(options("/api/bff/cursos")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:5173"));
        mvc.perform(options("/api/bff/cursos")
                        .header(HttpHeaders.ORIGIN, "http://origen-no-permitido.test")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
