package com.incentive.user.security;

import com.incentive.user.config.JwtProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

/** 集中生成访问令牌，调用方不需要了解 JWT claims 和签名细节。 */
@Service
public class JwtTokenService {
  private final JwtEncoder encoder;
  private final JwtProperties properties;
  private final Clock clock;

  public JwtTokenService(JwtEncoder encoder, JwtProperties properties, Clock clock) {
    this.encoder = encoder;
    this.properties = properties;
    this.clock = clock;
  }

  public IssuedAccessToken issue(Long userId) {
    Instant issuedAt = clock.instant();
    Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(properties.issuer())
        .audience(List.of(properties.audience()))
        .subject(userId.toString())
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .id(UUID.randomUUID().toString())
        .claim("roles", List.of("USER"))
        .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
    String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new IssuedAccessToken(token, properties.accessTokenTtl().toSeconds());
  }
}
