package com.bonchang.qerp.appauth;

import com.bonchang.qerp.account.Account;
import com.bonchang.qerp.account.CashBalance;
import com.bonchang.qerp.account.CashBalanceRepository;
import com.bonchang.qerp.appuser.AppUser;
import com.bonchang.qerp.appuser.AppUserService;
import com.bonchang.qerp.marketdata.MarketDataHealthResponse;
import com.bonchang.qerp.marketdata.MarketDataProperties;
import com.bonchang.qerp.marketdata.MarketDataStatusService;
import com.bonchang.qerp.security.AppPrincipal;
import com.bonchang.qerp.security.JwtTokenService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AppSessionService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

    private final AppUserService appUserService;
    private final CashBalanceRepository cashBalanceRepository;
    private final JwtTokenService jwtTokenService;
    private final MarketDataStatusService marketDataStatusService;
    private final MarketDataProperties marketDataProperties;

    @Transactional
    public GuestAuthResponse issueGuestSession() {
        AppUser user = appUserService.createGuestUser();
        return toGuestAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AppMeResponse currentSession() {
        AppPrincipal principal = requirePrincipalWithAccount();
        AppUser user = appUserService.getActiveUser(principal.userId());
        Account account = user.getAccount();
        CashBalance balance = cashBalanceRepository.findByAccountId(account.getId()).orElse(null);
        MarketDataHealthResponse health = marketDataStatusService.health(
                marketDataProperties.isEnabled(),
                marketDataProperties.getApiKey() != null && !marketDataProperties.getApiKey().isBlank()
        );
        return new AppMeResponse(
                user.getId(),
                user.getAuthType().name(),
                user.getRole(),
                user.getDisplayName(),
                new AppMeResponse.Account(
                        account.getId(),
                        account.getAccountCode(),
                        account.getOwnerName(),
                        account.getBaseCurrency(),
                        balance != null ? balance.getAvailableCash() : ZERO,
                        balance != null ? balance.getReservedCash() : ZERO
                ),
                new AppMeResponse.MarketConnection(
                        health.status(),
                        health.source(),
                        health.staleQuoteCount() > 0,
                        health.staleQuoteCount(),
                        health.lastQuoteReceivedAt()
                )
        );
    }

    @Transactional(readOnly = true)
    public Optional<AppPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    @Transactional(readOnly = true)
    public AppPrincipal requirePrincipalWithAccount() {
        AppPrincipal principal = currentPrincipal()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "authentication required"));
        if (!principal.hasAccount() || principal.userId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "app session required");
        }
        return principal;
    }

    private GuestAuthResponse toGuestAuthResponse(AppUser user) {
        String token = jwtTokenService.generateAppUserToken(user);
        return new GuestAuthResponse(
                token,
                "Bearer",
                jwtTokenService.expiresInSeconds(),
                user.getRole(),
                user.getAuthType().name(),
                user.getId(),
                user.getAccount().getId(),
                user.getDisplayName(),
                user.getAccount().getAccountCode()
        );
    }
}
