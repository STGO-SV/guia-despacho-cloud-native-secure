package com.duoc.eft.cursos.config;

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
        addLocalDemoRole(result, jwt.getSubject());
        addLocalDemoTokenRole(result, jwt.getTokenValue());
        return result;
    }
    private void addLocalDemoRole(Set<GrantedAuthority> result, String subject) {
        if ("local-instructor".equals(subject)) add(result, "INSTRUCTOR");
        else if ("local-estudiante".equals(subject)) add(result, "ESTUDIANTE");
    }
    private void addLocalDemoTokenRole(Set<GrantedAuthority> result, String token) {
        if ("demo-instructor".equals(token)) add(result, "INSTRUCTOR");
        else if ("demo-estudiante".equals(token)) add(result, "ESTUDIANTE");
    }
    private void add(Set<GrantedAuthority> result, String role) {
        String normalized = role.trim();
        if (normalized.startsWith("ROLE_")) normalized = normalized.substring(5);
        if (!normalized.isBlank()) result.add(new SimpleGrantedAuthority("ROLE_" + normalized));
    }
}
