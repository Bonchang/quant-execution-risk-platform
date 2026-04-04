package com.bonchang.qerp.security;

import com.bonchang.qerp.appuser.AppUser;
import com.bonchang.qerp.appuser.AppUserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final AppUserRepository appUserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtTokenService.parse(token);
                List<String> roles = jwtTokenService.extractRoles(token);
                AppUser appUser = resolveAppUser(claims);
                Long userId = appUser != null ? appUser.getId() : jwtTokenService.extractUserId(claims);
                Long accountId = appUser != null && appUser.getAccount() != null
                        ? appUser.getAccount().getId()
                        : jwtTokenService.extractAccountId(claims);
                String authType = appUser != null
                        ? appUser.getAuthType().name()
                        : jwtTokenService.extractAuthType(claims);
                String displayName = appUser != null
                        ? appUser.getDisplayName()
                        : jwtTokenService.extractDisplayName(claims);

                var authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
                var principal = new AppPrincipal(
                        userId,
                        accountId,
                        claims.getSubject(),
                        displayName != null ? displayName : claims.getSubject(),
                        authType != null ? authType : "OPS",
                        roles
                );
                var authentication = new UsernamePasswordAuthenticationToken(principal, token, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private AppUser resolveAppUser(Claims claims) {
        Long userId = jwtTokenService.extractUserId(claims);
        if (userId == null) {
            return null;
        }
        return appUserRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new IllegalArgumentException("app user not found"));
    }
}
