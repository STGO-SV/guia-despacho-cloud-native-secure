package com.duoc.eft.cursos.config;

import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@Configuration
@ConditionalOnProperty(name = "app.security.demo-enabled", havingValue = "true")
public class DemoJwtDecoderConfig {
    @Bean JwtDecoder demoJwtDecoder(@Value("${app.security.roles-claim}") String rolesClaim) {
        return token -> decode(token, rolesClaim);
    }

    private Jwt decode(String token, String rolesClaim) {
        String role = switch (token) {
            case "demo-instructor" -> "INSTRUCTOR";
            case "demo-estudiante" -> "ESTUDIANTE";
            default -> throw new JwtException("Token academico invalido");
        };
        Instant now = Instant.now();
        return Jwt.withTokenValue(token).header("alg", "none").subject("local-" + role.toLowerCase())
                .issuedAt(now).expiresAt(now.plusSeconds(3600)).claim(rolesClaim, List.of(role)).build();
    }
}
