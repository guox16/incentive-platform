package com.incentive.user.config;

import com.incentive.common.security.PlatformJwtConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@Import(PlatformJwtConfiguration.class)
public class UserSecurityConfiguration {
  @Bean
  SecurityFilterChain userSecurityFilterChain(HttpSecurity http, JwtDecoder platformJwtDecoder,
      JwtAuthenticationConverter platformJwtAuthenticationConverter) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(requests -> requests
            .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login",
                "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
            .requestMatchers("/actuator/health", "/actuator/info", "/v3/api-docs/**",
                "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .requestMatchers("/api/v1/users/admin/**").hasAuthority("ROLE_MANAGE")
            .requestMatchers("/api/v1/users/me").hasAuthority("ACCOUNT_SELF")
            .anyRequest().denyAll())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
            .decoder(platformJwtDecoder)
            .jwtAuthenticationConverter(platformJwtAuthenticationConverter)))
        .build();
  }
}
