package com.duoc.eft.bff.config;

import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter converter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource(
                        allowedOrigin)))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> error(res, 401, "Token ausente o invalido"))
                        .accessDeniedHandler((req, res, ex) -> error(res, 403, "Permiso insuficiente")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/bff/cursos").hasAnyRole("ESTUDIANTE", "INSTRUCTOR", "DESCARGA_GUIAS", "GESTION_GUIAS")
                        .requestMatchers(HttpMethod.POST, "/api/bff/cursos").hasAnyRole("INSTRUCTOR", "GESTION_GUIAS")
                        .requestMatchers(HttpMethod.POST, "/api/bff/inscripciones").hasAnyRole("ESTUDIANTE", "DESCARGA_GUIAS")
                        .requestMatchers(HttpMethod.POST, "/api/bff/inscripciones/consumir").hasAnyRole("INSTRUCTOR", "GESTION_GUIAS")
                        .requestMatchers(HttpMethod.GET, "/api/bff/inscripciones/*").hasAnyRole("ESTUDIANTE", "INSTRUCTOR", "DESCARGA_GUIAS", "GESTION_GUIAS")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)));
        return http.build();
    }
    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    CorsConfigurationSource corsConfigurationSource(String origin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(origin));
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/bff/**", configuration);
        return source;
    }
    @Bean JwtAuthenticationConverter jwtAuthenticationConverter(@Value("${app.security.roles-claim}") String claim) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtRolesConverter(claim));
        return converter;
    }
    @Bean @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${app.security.jwk-set-uri}") String jwk,
            @Value("${app.security.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwk).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), new AudienceValidator(audience)));
        return decoder;
    }
    private void error(jakarta.servlet.http.HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
