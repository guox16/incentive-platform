package com.incentive.points.config;

import com.incentive.common.security.InternalJwtProperties;
import com.incentive.common.security.JwtAudienceValidator;
import com.incentive.common.security.JwtPermissionConverter;
import com.incentive.common.security.JwtTrustedIssuerValidator;
import com.incentive.common.security.PlatformJwtConfiguration;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@Import(PlatformJwtConfiguration.class)
@EnableConfigurationProperties(InternalJwtProperties.class)
public class PointsSecurityConfiguration {
  @Bean
  JwtDecoder internalJwtDecoder(InternalJwtProperties properties,
      @Value("${security.internal-jwt.trusted-issuers:${security.internal-jwt.issuer}}")
      java.util.List<String> trustedIssuers) {
    SecretKey key = new SecretKeySpec(
        properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        new JwtTimestampValidator(), new JwtTrustedIssuerValidator(trustedIssuers),
        new JwtAudienceValidator(properties.audience())));
    return decoder;
  }

  @Bean
  @Order(1)
  SecurityFilterChain internalPointSecurityFilterChain(HttpSecurity http,
      @Qualifier("internalJwtDecoder") JwtDecoder decoder) throws Exception {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new JwtPermissionConverter());
    return http
        .securityMatcher("/api/v1/internal/points/**")
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(requests -> requests.anyRequest().hasAuthority("POINTS_COMMAND"))
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
            .decoder(decoder).jwtAuthenticationConverter(converter)))
        .build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain pointQuerySecurityFilterChain(HttpSecurity http,
      @Qualifier("platformJwtDecoder") JwtDecoder decoder,
      JwtAuthenticationConverter platformJwtAuthenticationConverter) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(requests -> requests
            .requestMatchers("/actuator/health", "/actuator/info", "/v3/api-docs/**",
                "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .requestMatchers("/api/v1/points/me/**").hasAuthority("POINTS_SELF")
            .anyRequest().denyAll())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
            .decoder(decoder)
            .jwtAuthenticationConverter(platformJwtAuthenticationConverter)))
        .build();
  }
}
