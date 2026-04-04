package com.bonchang.qerp.security;

import java.security.Principal;
import java.util.List;

public record AppPrincipal(
        Long userId,
        Long accountId,
        String username,
        String displayName,
        String authType,
        List<String> roles
) implements Principal {

    @Override
    public String getName() {
        return username;
    }

    public String primaryRole() {
        return roles == null || roles.isEmpty() ? "" : roles.get(0);
    }

    public boolean hasAccount() {
        return accountId != null;
    }
}
