package com.incentive.gateway.security;

import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** 删除外部伪造身份头，并仅从网关已验证的 JWT 写入可信用户 ID。 */
@Component
public class TrustedIdentityFilter implements GlobalFilter, Ordered {
  public static final String USER_ID_HEADER = "X-User-Id";
  public static final String USER_ROLES_HEADER = "X-User-Roles";
  public static final String USER_PERMISSIONS_HEADER = "X-User-Permissions";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerWebExchange sanitized = mutate(exchange, null, null, null);
    return exchange.getPrincipal()
        .filter(JwtAuthenticationToken.class::isInstance)
        .cast(JwtAuthenticationToken.class)
        .map(authentication -> {
          List<String> roles = authentication.getToken().getClaimAsStringList("roles");
          List<String> permissions = authentication.getToken().getClaimAsStringList("permissions");
          return mutate(exchange, authentication.getToken().getSubject(),
              roles == null ? null : String.join(",", roles),
              permissions == null ? null : String.join(",", permissions));
        })
        .defaultIfEmpty(sanitized)
        .flatMap(chain::filter);
  }

  private ServerWebExchange mutate(ServerWebExchange exchange, String userId, String roles,
      String permissions) {
    return exchange.mutate().request(request -> request.headers(headers -> {
      headers.remove(USER_ID_HEADER);
      headers.remove(USER_ROLES_HEADER);
      headers.remove(USER_PERMISSIONS_HEADER);
      if (userId != null) headers.set(USER_ID_HEADER, userId);
      if (roles != null && !roles.isBlank()) headers.set(USER_ROLES_HEADER, roles);
      if (permissions != null && !permissions.isBlank()) {
        headers.set(USER_PERMISSIONS_HEADER, permissions);
      }
    })).build();
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
