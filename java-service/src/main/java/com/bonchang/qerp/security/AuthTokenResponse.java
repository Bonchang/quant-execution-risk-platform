package com.bonchang.qerp.security;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String role
) {
}
