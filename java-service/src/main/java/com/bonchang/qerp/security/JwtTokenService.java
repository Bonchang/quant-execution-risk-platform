package com.bonchang.qerp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final SecretKey signingKey;
    private final Duration tokenTtl;

    public JwtTokenService(
            @Value("${security.jwt.secret:qerp-demo-secret-key-qerp-demo-secret-key}") String secret,
            @Value("${security.jwt.ttl-seconds:3600}") long ttlSeconds
    ) {
        byte[] keyBytes = secret.length() >= 32
                ? secret.getBytes(StandardCharsets.UTF_8)
                : Decoders.BASE64.decode("cWVycC1kZW1vLXNlY3JldC1rZXktcWVycC1kZW1vLXNlY3JldC1rZXk=");
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.tokenTtl = Duration.ofSeconds(ttlSeconds);
    }

    public String generateToken(UserDetails userDetails) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", authorities(userDetails.getAuthorities()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(tokenTtl)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long expiresInSeconds() {
        return tokenTtl.toSeconds();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object value = parse(token).get("roles");
        if (value instanceof List<?> roles) {
            return roles.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private List<String> authorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).toList();
    }
}
