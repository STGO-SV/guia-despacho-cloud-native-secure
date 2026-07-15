package com.duoc.eft.cursos.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duoc.eft.cursos.config.SecurityConfig;
import com.duoc.eft.cursos.dto.CursoResponse;
import com.duoc.eft.cursos.service.CursoService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CursoController.class)
@Import(SecurityConfig.class)
class CursoControllerTests {
    @Autowired MockMvc mvc;
    @MockitoBean CursoService service;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/cursos")).andExpect(status().isUnauthorized());
    }
    @Test void estudianteNoPuedeCrear() throws Exception {
        mvc.perform(post("/api/cursos").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ESTUDIANTE")))
                .contentType(MediaType.APPLICATION_JSON).content(jsonValido()))
                .andExpect(status().isForbidden());
    }
    @Test void instructorCreaCursoJson() throws Exception {
        when(service.crear(any())).thenReturn(new CursoResponse(1L, "Cloud", "Curso EFT", "Docente", "ACTIVO", null));
        mvc.perform(post("/api/cursos").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                .contentType(MediaType.APPLICATION_JSON).content(jsonValido()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.titulo").value("Cloud"));
    }
    @Test void validaJson() throws Exception {
        mvc.perform(post("/api/cursos").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
    @Test void estudianteListaCursos() throws Exception {
        when(service.listar()).thenReturn(List.of(new CursoResponse(1L, "Cloud", "Curso EFT", "Docente", "ACTIVO", null)));
        mvc.perform(get("/api/cursos").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
    }
    private String jsonValido() {
        return "{\"titulo\":\"Cloud\",\"descripcion\":\"Curso EFT\",\"instructor\":\"Docente\",\"estado\":\"ACTIVO\"}";
    }
}

