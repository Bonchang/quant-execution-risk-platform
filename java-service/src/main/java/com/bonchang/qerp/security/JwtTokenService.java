package com.bonchang.qerp.security;

import com.bonchang.qerp.appuser.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final SecretKey signingKey;
    private final Duration tokenTtl;

    public JwtTokenService(
            @Value("${security.jwt.secret:}") String secret,
            Environment environment,
            @Value("${security.jwt.ttl-seconds:3600}") long ttlSeconds
    ) {
        String resolvedSecret = resolveSecret(secret, environment);
        this.signingKey = Keys.hmacShaKeyFor(resolvedSecret.getBytes(StandardCharsets.UTF_8));
        this.tokenTtl = Duration.ofSeconds(ttlSeconds);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(
                userDetails.getUsername(),
                authorities(userDetails.getAuthorities()),
                null,
                null,
                "OPS",
                userDetails.getUsername()
        );
    }

    public String generateAppUserToken(AppUser user) {
        return generateToken(
                "guest:" + user.getId(),
                List.of(user.getRole()),
                user.getId(),
                user.getAccount() != null ? user.getAccount().getId() : null,
                user.getAuthType().name(),
                user.getDisplayName()
        );
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

    public Long extractUserId(Claims claims) {
        return claims.get("userId", Long.class);
    }

    public Long extractAccountId(Claims claims) {
        return claims.get("accountId", Long.class);
    }

    public String extractAuthType(Claims claims) {
        return claims.get("authType", String.class);
    }

    public String extractDisplayName(Claims claims) {
        return claims.get("displayName", String.class);
    }

    private String generateToken(
            String subject,
            List<String> roles,
            Long userId,
            Long accountId,
            String authType,
            String displayName
    ) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .claim("authType", authType)
                .claim("displayName", displayName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(tokenTtl)))
                .signWith(signingKey);
        if (userId != null) {
            builder.claim("userId", userId);
        }
        if (accountId != null) {
            builder.claim("accountId", accountId);
        }
        return builder.compact();
    }

    private List<String> authorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).toList();
    }

    private String resolveSecret(String secret, Environment environment) {
        if (secret != null && !secret.isBlank()) {
            if (secret.length() < 32) {
                throw new IllegalStateException("security.jwt.secret must be at least 32 characters");
            }
            return secret;
        }
        List<String> activeProfiles = List.of(environment.getActiveProfiles());
        if (activeProfiles.contains("local") || activeProfiles.contains("test")) {
            return "qerp-local-fallback-secret-qerp-local-fallback";
        }
        throw new IllegalStateException("security.jwt.secret must be configured outside local/test");
    }
}
