package com.duoc.eft.bff.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private final String claim;
    public JwtRolesConverter(String claim) { this.claim = claim; }
    @Override public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object value = jwt.getClaim(claim);
        Set<GrantedAuthority> result = new LinkedHashSet<>();
        if (value instanceof String role) add(result, role);
        else if (value instanceof Collection<?> roles) roles.forEach(role -> add(result, String.valueOf(role)));
        return result;
    }
    private void add(Set<GrantedAuthority> result, String role) {
        String normalized = role.trim();
        if (normalized.startsWith("ROLE_")) normalized = normalized.substring(5);
        if (!normalized.isBlank()) result.add(new SimpleGrantedAuthority("ROLE_" + normalized));
    }
}

