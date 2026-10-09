package com.northstar.crm.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues signed JWTs (HS256) carrying the username (sub) and roles. */
@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final Duration ttl;

    public TokenService(JwtEncoder encoder, AuthProperties props) {
        this.encoder = encoder;
        this.ttl = props.jwt().ttl();
    }

    public String issue(Authentication auth) {
        Instant now = Instant.now();
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))           // skip Spring's FACTOR_* authorities
                .map(a -> a.replaceFirst("^ROLE_", ""))      // ROLE_AGENT -> AGENT
                .toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("northstar-crm")
                .subject(auth.getName())                     // "agent1" -> becomes the actor
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("roles", roles)
                .build();
        // Must name HS256 explicitly, or the encoder looks for an RSA key and fails.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
