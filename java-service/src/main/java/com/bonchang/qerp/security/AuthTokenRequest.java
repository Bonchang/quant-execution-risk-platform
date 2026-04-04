package com.bonchang.qerp.security;

import jakarta.validation.constraints.NotBlank;

public record AuthTokenRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
