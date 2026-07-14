package com.duoc.guia_despacho.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duoc.guia_despacho.config.JwtRolesConverter;
import com.duoc.guia_despacho.config.SecurityConfig;
import com.duoc.guia_despacho.dto.CrearGuiaRequest;
import com.duoc.guia_despacho.dto.GuiaDespachoResponse;
import com.duoc.guia_despacho.service.GuiaDespachoService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.example.com/test/v2.0/",
        "app.security.jwk-set-uri=https://login.example.com/test/discovery/v2.0/keys",
        "app.security.audience=cliente-test",
        "app.security.roles-claim=roles",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "management.health.rabbit.enabled=false"
})
class GuiaDespachoControllerSecurityTests {

    private static final String CREAR_GUIA_JSON = """
            {
              "numeroGuia": "GD-001",
              "transportista": "Transportes Norte",
              "fecha": "2026-06-29",
              "destinatario": "Cliente Demo",
              "direccionDestino": "Av. Siempre Viva 123",
              "descripcionCarga": "Cajas con insumos"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GuiaDespachoService guiaService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void postGuiasSinJwtDevuelve401() throws Exception {
        mockMvc.perform(post("/api/guias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREAR_GUIA_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postGuiasConGestionGuiasSuperaAutorizacion() throws Exception {
        when(guiaService.crearGuia(any(CrearGuiaRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/guias")
                        .with(jwtConRol("GESTION_GUIAS"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREAR_GUIA_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void postGuiasConDescargaGuiasDevuelve403() throws Exception {
        mockMvc.perform(post("/api/guias")
                        .with(jwtConRol("DESCARGA_GUIAS"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREAR_GUIA_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void postGuiasConTokenSinRolesDevuelve403() throws Exception {
        mockMvc.perform(post("/api/guias")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .issuer("https://login.example.com/test/v2.0/")
                                        .audience(List.of("cliente-test")))
                                .authorities(new JwtRolesConverter("roles")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREAR_GUIA_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void descargarConDescargaGuiasSuperaAutorizacion() throws Exception {
        when(guiaService.descargarGuia(eq(1L), eq("Transportes Norte"))).thenReturn("pdf".getBytes());

        mockMvc.perform(get("/api/guias/1/descargar")
                        .with(jwtConRol("DESCARGA_GUIAS"))
                        .param("transportista", "Transportes Norte"))
                .andExpect(status().isOk());
    }

    @Test
    void descargarConGestionGuiasDevuelve403() throws Exception {
        mockMvc.perform(get("/api/guias/1/descargar")
                        .with(jwtConRol("GESTION_GUIAS"))
                        .param("transportista", "Transportes Norte"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getGuiasConGestionGuiasSuperaAutorizacion() throws Exception {
        when(guiaService.buscarPorTransportistaYFecha(eq("Transportes Norte"), eq(LocalDate.parse("2026-06-29"))))
                .thenReturn(List.of(response()));

        mockMvc.perform(get("/api/guias")
                        .with(jwtConRol("GESTION_GUIAS"))
                        .param("transportista", "Transportes Norte")
                        .param("fecha", "2026-06-29"))
                .andExpect(status().isOk());
    }

    @Test
    void getGuiasConDescargaGuiasDevuelve403() throws Exception {
        mockMvc.perform(get("/api/guias")
                        .with(jwtConRol("DESCARGA_GUIAS"))
                        .param("transportista", "Transportes Norte")
                        .param("fecha", "2026-06-29"))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicarProcesamientoConGestionDevuelve202() throws Exception {
        mockMvc.perform(post("/api/procesamiento/guias/1")
                        .with(jwtConRol("GESTION_GUIAS")))
                .andExpect(status().isAccepted());
    }

    @Test
    void publicarProcesamientoConDescargaDevuelve403() throws Exception {
        mockMvc.perform(post("/api/procesamiento/guias/1")
                        .with(jwtConRol("DESCARGA_GUIAS")))
                .andExpect(status().isForbidden());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor jwtConRol(String rol) {
        return jwt()
                .jwt(jwt -> jwt
                        .issuer("https://login.example.com/test/v2.0/")
                        .audience(List.of("cliente-test"))
                        .claim("roles", List.of(rol)))
                .authorities(new JwtRolesConverter("roles"));
    }

    private static GuiaDespachoResponse response() {
        return new GuiaDespachoResponse(
                1L,
                "GD-001",
                "Transportes Norte",
                LocalDate.parse("2026-06-29"),
                "Cliente Demo",
                "Av. Siempre Viva 123",
                "Cajas con insumos",
                "CREADA",
                "guia-1.pdf",
                "./efs/guias/guia-1.pdf",
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
