package com.incentive.gateway.security;

import java.util.Collection;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

final class JwtPermissionConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    List<String> permissions = jwt.getClaimAsStringList("permissions");
    if (permissions == null) return List.of();
    return permissions.stream()
        .<GrantedAuthority>map(SimpleGrantedAuthority::new)
        .toList();
  }
}
