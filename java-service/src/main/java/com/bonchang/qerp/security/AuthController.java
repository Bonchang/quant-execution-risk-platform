package com.bonchang.qerp.security;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/token")
    @ResponseStatus(HttpStatus.OK)
    public AuthTokenResponse issueToken(@Valid @RequestBody AuthTokenRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        String token = jwtTokenService.generateToken(principal);
        String role = principal.getAuthorities().stream().findFirst().map(authority -> authority.getAuthority()).orElse("ROLE_VIEWER");
        return new AuthTokenResponse(token, "Bearer", jwtTokenService.expiresInSeconds(), role);
    }
}
