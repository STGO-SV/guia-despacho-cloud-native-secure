package com.duoc.guia_despacho.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedAudience;

    public AudienceValidator(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (!StringUtils.hasText(expectedAudience)) {
            OAuth2Error error = new OAuth2Error("invalid_token", "La audiencia esperada no esta configurada", null);
            return OAuth2TokenValidatorResult.failure(error);
        }

        Set<String> audiences = resolveAudiences(token);
        if (audiences.contains(expectedAudience.trim())) {
            return OAuth2TokenValidatorResult.success();
        }

        OAuth2Error error = new OAuth2Error("invalid_token", "La audiencia del token no es valida", null);
        return OAuth2TokenValidatorResult.failure(error);
    }

    private Set<String> resolveAudiences(Jwt token) {
        Set<String> audiences = new LinkedHashSet<>();

        List<String> jwtAudiences = token.getAudience();
        if (jwtAudiences != null) {
            jwtAudiences.forEach(audience -> addAudience(audiences, audience));
        }

        Object rawAudience = token.getClaim("aud");
        if (rawAudience instanceof String audience) {
            addAudience(audiences, audience);
        } else if (rawAudience instanceof Collection<?> values) {
            values.forEach(value -> addAudience(audiences, value));
        }

        return audiences;
    }

    private void addAudience(Set<String> audiences, Object value) {
        if (value instanceof String audience && StringUtils.hasText(audience)) {
            audiences.add(audience.trim());
        }
    }
}
