package com.bonchang.qerp.appuser;

import com.bonchang.qerp.account.Account;
import com.bonchang.qerp.account.AccountService;
import com.bonchang.qerp.strategyrun.StrategyRun;
import com.bonchang.qerp.strategyrun.StrategyRunRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final AccountService accountService;
    private final StrategyRunRepository strategyRunRepository;

    @Value("${app-user.guest-initial-cash:100000.000000}")
    private BigDecimal guestInitialCash;

    @Value("${app-user.guest-retention-days:7}")
    private long guestRetentionDays;

    @Transactional
    public AppUser createGuestUser() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        Account account = accountService.createPaperAccount(
                "GUEST-" + suffix,
                "Guest Trader " + suffix,
                guestInitialCash
        );
        ensureDefaultConsumerStrategyRun(account);

        AppUser user = new AppUser();
        user.setAccount(account);
        user.setAuthType(AppUserAuthType.GUEST);
        user.setDisplayName("Guest " + suffix);
        user.setRole("ROLE_GUEST");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        return appUserRepository.save(user);
    }

    @Transactional
    public AppUser touchGuest(Long userId) {
        AppUser user = getActiveUser(userId);
        user.setLastLoginAt(LocalDateTime.now());
        return appUserRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AppUser getActiveUser(Long userId) {
        return appUserRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new IllegalArgumentException("app user not found"));
    }

    @Scheduled(fixedDelayString = "${app-user.purge-poll-ms:3600000}")
    @Transactional
    public void deactivateInactiveGuests() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(Math.max(1L, guestRetentionDays));
        List<AppUser> staleGuests = appUserRepository.findByAuthTypeAndLastLoginAtBeforeAndActiveTrue(AppUserAuthType.GUEST, threshold);
        for (AppUser guest : staleGuests) {
            guest.setActive(false);
        }
    }

    public StrategyRun ensureDefaultConsumerStrategyRun(Account account) {
        return strategyRunRepository.findFirstByAccountIdAndStrategyNameOrderByRunAtDescIdDesc(account.getId(), "consumer-default")
                .orElseGet(() -> {
                    StrategyRun strategyRun = new StrategyRun();
                    strategyRun.setAccount(account);
                    strategyRun.setStrategyName("consumer-default");
                    strategyRun.setRunAt(LocalDateTime.now());
                    strategyRun.setParametersJson("{\"origin\":\"guest-session\",\"mode\":\"paper-trading\"}");
                    return strategyRunRepository.save(strategyRun);
                });
    }
}
