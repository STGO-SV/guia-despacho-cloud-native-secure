package com.duoc.guia_despacho.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtRolesConverterTests {

    private final JwtRolesConverter converter = new JwtRolesConverter("roles");
    private final JwtRolesConverter customClaimConverter =
            new JwtRolesConverter("extension_RolGuia");

    @Test
    void tokenSinRolesNoConcedeAuthorities() {
        Collection<GrantedAuthority> authorities = converter.convert(jwt(Map.of()));

        assertThat(authorities).isEmpty();
    }

    @Test
    void claimRolesComoListaSeConvierteARoleAuthorities() {
        Collection<GrantedAuthority> authorities = converter.convert(jwt(Map.of(
                "roles", List.of("GESTION_GUIAS", "DESCARGA_GUIAS")
        )));

        assertThat(authorityNames(authorities))
                .containsExactlyInAnyOrder("ROLE_GESTION_GUIAS", "ROLE_DESCARGA_GUIAS");
    }

    @Test
    void claimRolesComoStringSeConvierteARoleAuthority() {
        Collection<GrantedAuthority> authorities = converter.convert(jwt(Map.of(
                "roles", "GESTION_GUIAS"
        )));

        assertThat(authorityNames(authorities)).containsExactly("ROLE_GESTION_GUIAS");
    }

    @Test
    void claimPersonalizadoRealComoStringSeConvierteARoleAuthority() {
        Collection<GrantedAuthority> authorities = customClaimConverter.convert(jwt(Map.of(
                "extension_RolGuia", "GESTION_GUIAS"
        )));

        assertThat(authorityNames(authorities)).containsExactly("ROLE_GESTION_GUIAS");
    }

    @Test
    void rolConEspaciosAccidentalesSeLimpia() {
        Collection<GrantedAuthority> authorities = converter.convert(jwt(Map.of(
                "roles", List.of("  GESTION_GUIAS  ")
        )));

        assertThat(authorityNames(authorities)).containsExactly("ROLE_GESTION_GUIAS");
    }

    @Test
    void rolYaPrefijadoNoDuplicaRole() {
        Collection<GrantedAuthority> authorities = converter.convert(jwt(Map.of(
                "roles", List.of("ROLE_GESTION_GUIAS")
        )));

        assertThat(authorityNames(authorities)).containsExactly("ROLE_GESTION_GUIAS");
    }

    @Test
    void ignoraRolesVaciosYEvitaDuplicados() {
        Collection<GrantedAuthority> authorities = converter.convert(jwt(Map.of(
                "roles", List.of("GESTION_GUIAS", " ", "ROLE_GESTION_GUIAS")
        )));

        assertThat(authorityNames(authorities)).containsExactly("ROLE_GESTION_GUIAS");
    }

    @Test
    void claimRolesComoArregloSeConvierteARoleAuthorities() {
        Collection<GrantedAuthority> authorities = converter.convert(jwt(Map.of(
                "roles", new String[]{"GESTION_GUIAS", "DESCARGA_GUIAS"}
        )));

        assertThat(authorityNames(authorities))
                .containsExactlyInAnyOrder("ROLE_GESTION_GUIAS", "ROLE_DESCARGA_GUIAS");
    }

    @Test
    void noConvierteScopesEnRoles() {
        Collection<GrantedAuthority> authorities = converter.convert(jwt(Map.of(
                "scp", "GESTION_GUIAS"
        )));

        assertThat(authorities).isEmpty();
    }

    private static List<String> authorityNames(Collection<GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
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
