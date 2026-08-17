package com.incentive.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class TrustedIdentityFilterTest {
  private final TrustedIdentityFilter filter = new TrustedIdentityFilter();

  @Test
  void replacesSpoofedIdentityWithVerifiedJwtSubject() {
    Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
        Map.of("alg", "HS256"), Map.of("sub", "42", "roles", List.of("USER")));
    var authentication = new JwtAuthenticationToken(jwt);
    var request = MockServerHttpRequest.get("/api/v1/users/me")
        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
        .header(TrustedIdentityFilter.USER_ID_HEADER, "999")
        .header(TrustedIdentityFilter.USER_ROLES_HEADER, "ADMIN")
        .build();
    var delegate = MockServerWebExchange.from(request);
    ServerWebExchange exchange = new org.springframework.web.server.ServerWebExchangeDecorator(delegate) {
      @Override
      @SuppressWarnings("unchecked")
      public <T extends java.security.Principal> Mono<T> getPrincipal() {
        return Mono.just((T) authentication);
      }
    };
    AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    GatewayFilterChain chain = next -> {
      forwarded.set(next);
      return Mono.empty();
    };

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(forwarded.get().getRequest().getHeaders()
        .getFirst(TrustedIdentityFilter.USER_ID_HEADER)).isEqualTo("42");
    assertThat(forwarded.get().getRequest().getHeaders()
        .getFirst(TrustedIdentityFilter.USER_ROLES_HEADER)).isEqualTo("USER");
  }

  @Test
  void removesSpoofedIdentityFromUnauthenticatedRequest() {
    var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/auth/login")
        .header(TrustedIdentityFilter.USER_ID_HEADER, "999")
        .build());
    AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    GatewayFilterChain chain = next -> {
      forwarded.set(next);
      return Mono.empty();
    };

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(forwarded.get().getRequest().getHeaders()
        .containsKey(TrustedIdentityFilter.USER_ID_HEADER)).isFalse();
  }
}
