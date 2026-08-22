package com.incentive.gateway.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableConfigurationProperties(GatewayJwtProperties.class)
public class GatewaySecurityConfiguration {
  @Bean
  ReactiveJwtDecoder jwtDecoder(GatewayJwtProperties properties,
      ReactiveAccessTokenBlacklist blacklist) {
    SecretKey key = new SecretKeySpec(
        properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(key)
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
    OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(properties.issuer());
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        issuer, new JwtAudienceValidator(properties.audience())));
    return token -> decoder.decode(token).flatMap(blacklist::validate);
  }

  @Bean
  SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
      GatewaySecurityErrorWriter errors) {
    JwtAuthenticationConverter permissions = new JwtAuthenticationConverter();
    permissions.setJwtGrantedAuthoritiesConverter(new JwtPermissionConverter());
    return http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login",
                "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
            .pathMatchers("/actuator/health", "/actuator/info", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
            .pathMatchers("/api/v1/internal/**").denyAll()
            .pathMatchers("/api/v1/users/admin/**").hasAuthority("ROLE_MANAGE")
            .pathMatchers("/api/v1/activities/admin/**").hasAuthority("ACTIVITY_MANAGE")
            .pathMatchers("/api/v1/awards/*/inventory-adjustments",
                "/api/v1/awards/*/inventory-ledgers").hasAuthority("INVENTORY_MANAGE")
            .pathMatchers("/api/v1/awards/**").hasAuthority("PRIZE_MANAGE")
            .pathMatchers("/api/v1/points/me/**").hasAuthority("POINTS_SELF")
            .pathMatchers("/api/v1/activities/check-ins/me/**").hasAuthority("CHECK_IN")
            .pathMatchers("/api/v1/activities/lotteries/**").hasAuthority("LOTTERY_PARTICIPATE")
            .pathMatchers("/api/v1/activities/redemptions/**").hasAuthority("REDEMPTION_PARTICIPATE")
            .pathMatchers("/api/v1/users/me").hasAuthority("ACCOUNT_SELF")
            .pathMatchers("/api/v1/**").authenticated()
            .anyExchange().permitAll())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(
                new ReactiveJwtAuthenticationConverterAdapter(permissions)))
            .authenticationEntryPoint((exchange, exception) -> errors.write(
                exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录或重新登录")))
        .exceptionHandling(exceptions -> exceptions
            .accessDeniedHandler((exchange, exception) -> errors.write(
                exchange, HttpStatus.FORBIDDEN, "FORBIDDEN", "无权访问该资源")))
        .build();
  }
}
