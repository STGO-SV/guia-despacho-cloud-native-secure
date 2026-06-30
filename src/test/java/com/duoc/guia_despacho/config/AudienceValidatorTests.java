package com.duoc.guia_despacho.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class AudienceValidatorTests {

    @Test
    void audienciaCorrectaComoListaEsAceptada() {
        OAuth2TokenValidatorResult result = new AudienceValidator("cliente-test")
                .validate(jwt(Map.of("aud", List.of("cliente-test"))));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void audienciaCorrectaComoStringEsAceptada() {
        OAuth2TokenValidatorResult result = new AudienceValidator("cliente-test")
                .validate(jwt(Map.of("aud", "cliente-test")));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void audienciaIncorrectaEsRechazada() {
        OAuth2TokenValidatorResult result = new AudienceValidator("cliente-test")
                .validate(jwt(Map.of("aud", List.of("otro-cliente"))));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void audienciaAusenteEsRechazada() {
        OAuth2TokenValidatorResult result = new AudienceValidator("cliente-test")
                .validate(jwt(Map.of()));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void audienciaConfiguradaVaciaEsRechazada() {
        OAuth2TokenValidatorResult result = new AudienceValidator(" ")
                .validate(jwt(Map.of("aud", List.of("cliente-test"))));

        assertThat(result.hasErrors()).isTrue();
    }

    private static Jwt jwt(Map<String, Object> claims) {
        Map<String, Object> jwtClaims = new java.util.LinkedHashMap<>();
        jwtClaims.put("sub", "usuario-test");
        jwtClaims.putAll(claims);

        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                jwtClaims
        );
    }
}
