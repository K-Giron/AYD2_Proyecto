package com.megastore.sgologic.security;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Emite los JWT de acceso (BB-07, ADR-013). El token incluye el sujeto, los
 * roles y una vigencia configurable; se firma con HMAC-SHA256.
 */
@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;
    private final Clock clock;

    public JwtTokenService(JwtEncoder jwtEncoder, SecurityProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public String issueToken(String username, List<String> roles) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getJwt().getIssuer())
                .issuedAt(now)
                .expiresAt(now.plus(properties.getJwt().getTtl()))
                .subject(username)
                .id(UUID.randomUUID().toString())
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Instant expiresAt() {
        return clock.instant().plus(properties.getJwt().getTtl());
    }
}
