package com.bonchang.qerp.appauth;

public record GuestAuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String role,
        String authType,
        Long userId,
        Long accountId,
        String displayName,
        String accountCode
) {
}
