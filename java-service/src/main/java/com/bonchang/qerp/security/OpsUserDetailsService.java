package com.bonchang.qerp.security;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpsUserDetailsService implements UserDetailsService {

    private final OpsUserProperties opsUserProperties;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return opsUserProperties.getUsers().stream()
                .filter(user -> user.getUsername() != null && user.getUsername().equals(username))
                .findFirst()
                .map(user -> User.withUsername(user.getUsername())
                        .password(passwordEncoder.encode(user.getPassword()))
                        .roles(normalizeRole(user.getRole()))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("ops user not found"));
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "VIEWER";
        }
        return role.replace("ROLE_", "").toUpperCase(Locale.ROOT);
    }
}
