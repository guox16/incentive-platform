package com.incentive.activity.config;

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
public class IncentiveSecurityConfiguration {
  @Bean
  SecurityFilterChain incentiveSecurityFilterChain(HttpSecurity http, JwtDecoder platformJwtDecoder,
      JwtAuthenticationConverter platformJwtAuthenticationConverter) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(requests -> requests
            .requestMatchers("/actuator/health", "/actuator/info", "/v3/api-docs/**",
                "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .requestMatchers("/api/v1/activities/admin/**").hasAuthority("ACTIVITY_MANAGE")
            .requestMatchers("/api/v1/activities/check-ins/me/**").hasAuthority("CHECK_IN")
            .requestMatchers("/api/v1/activities/lotteries/**").hasAuthority("LOTTERY_PARTICIPATE")
            .requestMatchers("/api/v1/activities/redemptions/**").hasAuthority("REDEMPTION_PARTICIPATE")
            .requestMatchers("/api/v1/activities/**").authenticated()
            .anyRequest().denyAll())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
            .decoder(platformJwtDecoder)
            .jwtAuthenticationConverter(platformJwtAuthenticationConverter)))
        .build();
  }
}
