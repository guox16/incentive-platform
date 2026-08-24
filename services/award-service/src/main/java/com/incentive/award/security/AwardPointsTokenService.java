package com.incentive.award.security;

import com.incentive.common.security.InternalJwtProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class AwardPointsTokenService {
  private final JwtEncoder encoder;
  private final InternalJwtProperties properties;
  private final Clock clock;

  public AwardPointsTokenService(
      @Qualifier("awardInternalJwtEncoder") JwtEncoder encoder,
      InternalJwtProperties properties, Clock clock) {
    this.encoder = encoder;
    this.properties = properties;
    this.clock = clock;
  }

  public String issue() {
    Instant issuedAt = clock.instant();
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(properties.issuer())
        .audience(List.of(properties.audience()))
        .subject(properties.issuer())
        .issuedAt(issuedAt)
        .expiresAt(issuedAt.plus(properties.tokenTtl()))
        .id(UUID.randomUUID().toString())
        .claim("permissions", List.of("POINTS_COMMAND"))
        .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
