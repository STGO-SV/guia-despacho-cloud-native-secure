package com.duoc.guia_despacho.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

public class JwtRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String rolesClaim;

    public JwtRolesConverter(String rolesClaim) {
        this.rolesClaim = rolesClaim;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object claimValue = jwt.getClaim(rolesClaim);
        Set<String> roles = new LinkedHashSet<>();

        if (claimValue instanceof String role) {
            addRole(roles, role);
        } else if (claimValue instanceof Collection<?> values) {
            values.forEach(value -> addRole(roles, value));
        } else if (claimValue instanceof Object[] values) {
            for (Object value : values) {
                addRole(roles, value);
            }
        }

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toUnmodifiableSet());
    }

    private void addRole(Set<String> roles, Object value) {
        if (value instanceof String role && StringUtils.hasText(role)) {
            String normalizedRole = role.trim();
            if (normalizedRole.startsWith("ROLE_")) {
                normalizedRole = normalizedRole.substring("ROLE_".length());
            }
            if (StringUtils.hasText(normalizedRole)) {
                roles.add(normalizedRole);
            }
        }
    }
}
