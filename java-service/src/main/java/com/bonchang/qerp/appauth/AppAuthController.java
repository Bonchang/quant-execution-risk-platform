package com.bonchang.qerp.appauth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
public class AppAuthController {

    private final AppSessionService appSessionService;

    @PostMapping("/auth/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public GuestAuthResponse issueGuestSession() {
        return appSessionService.issueGuestSession();
    }

    @GetMapping("/me")
    public AppMeResponse me() {
        return appSessionService.currentSession();
    }
}
