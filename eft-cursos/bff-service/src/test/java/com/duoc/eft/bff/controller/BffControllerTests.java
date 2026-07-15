package com.duoc.eft.bff.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duoc.eft.bff.client.CursosClient;
import com.duoc.eft.bff.client.InscripcionesClient;
import com.duoc.eft.bff.config.SecurityConfig;
import java.net.ConnectException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
        String json = "{\"cursoId\":1,\"estudianteId\":\"e1\"}";
        when(inscripciones.crear(json, "Bearer token-demo"))
                .thenReturn(ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(json));
        mvc.perform(post("/api/bff/inscripciones").header("Authorization", "Bearer token-demo")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        verify(inscripciones).crear(json, "Bearer token-demo");
    }
    @Test void manejaServicioNoDisponible() throws Exception {
        when(cursos.listar(null)).thenThrow(new ResourceAccessException("sin conexion", new ConnectException()));
        mvc.perform(get("/api/bff/cursos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"))))
                .andExpect(status().isServiceUnavailable());
    }
}
