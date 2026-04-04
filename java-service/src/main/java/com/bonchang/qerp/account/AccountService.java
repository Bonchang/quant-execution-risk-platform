package com.bonchang.qerp.account;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

    private final AccountRepository accountRepository;
    private final CashBalanceRepository cashBalanceRepository;
    private final CashLedgerEntryRepository cashLedgerEntryRepository;

    public boolean exists(Long accountId) {
        return accountRepository.existsById(accountId);
    }

    public Account getReferenceAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "accountId not found"));
    }

    public List<AccountSummaryResponse> listAccounts() {
        return accountRepository.findAll(PageRequest.of(0, 100)).stream()
                .map(account -> {
                    CashBalance balance = cashBalanceRepository.findByAccountId(account.getId()).orElse(null);
                    return new AccountSummaryResponse(
                            account.getId(),
                            account.getAccountCode(),
                            account.getOwnerName(),
                            account.getBaseCurrency(),
                            balance != null ? balance.getAvailableCash() : ZERO,
                            balance != null ? balance.getReservedCash() : ZERO,
                            balance != null ? balance.getUpdatedAt() : null
                    );
                })
                .toList();
    }

    @Transactional
    public Account ensureDemoAccount(String accountCode, String ownerName, BigDecimal initialCash) {
        Account account = accountRepository.findByAccountCode(accountCode).orElseGet(() -> {
            Account created = new Account();
            created.setAccountCode(accountCode);
            created.setOwnerName(ownerName);
            created.setBaseCurrency("USD");
            created.setCreatedAt(LocalDateTime.now());
            return accountRepository.save(created);
        });
        cashBalanceRepository.findByAccountId(account.getId()).orElseGet(() -> {
            CashBalance balance = new CashBalance();
            balance.setAccount(account);
            balance.setAvailableCash(initialCash.setScale(6, RoundingMode.HALF_UP));
            balance.setReservedCash(ZERO);
            balance.setUpdatedAt(LocalDateTime.now());
            return cashBalanceRepository.save(balance);
        });
        return account;
    }

    @Transactional
    public CashBalance lockCashBalance(Long accountId) {
        return cashBalanceRepository.lockByAccountId(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "cash balance not found for accountId"));
    }

    @Transactional
    public void reserveBuyCash(Account account, com.bonchang.qerp.order.Order order, BigDecimal amount, String note) {
        if (amount.compareTo(ZERO) <= 0) {
            return;
        }
        CashBalance balance = lockCashBalance(account.getId());
        BigDecimal normalized = amount.setScale(6, RoundingMode.HALF_UP);
        if (balance.getAvailableCash().compareTo(normalized) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "available cash is insufficient");
        }
        balance.setAvailableCash(balance.getAvailableCash().subtract(normalized).setScale(6, RoundingMode.HALF_UP));
        balance.setReservedCash(balance.getReservedCash().add(normalized).setScale(6, RoundingMode.HALF_UP));
        balance.setUpdatedAt(LocalDateTime.now());
        persistLedger(account, order, CashLedgerEntryType.RESERVE_BUY, normalized.negate(), balance, note);
        order.setReservedCashAmount(order.getReservedCashAmount().add(normalized).setScale(6, RoundingMode.HALF_UP));
    }

    @Transactional
    public void settleBuyFill(Account account, com.bonchang.qerp.order.Order order, BigDecimal fillNotional, String note) {
        BigDecimal normalized = fillNotional.setScale(6, RoundingMode.HALF_UP);
        if (normalized.compareTo(ZERO) <= 0) {
            return;
        }
        CashBalance balance = lockCashBalance(account.getId());
        balance.setReservedCash(balance.getReservedCash().subtract(normalized).max(ZERO).setScale(6, RoundingMode.HALF_UP));
        balance.setUpdatedAt(LocalDateTime.now());
        persistLedger(account, order, CashLedgerEntryType.BUY_FILL_SETTLEMENT, normalized.negate(), balance, note);
        order.setReservedCashAmount(order.getReservedCashAmount().subtract(normalized).max(ZERO).setScale(6, RoundingMode.HALF_UP));
    }

    @Transactional
    public void releaseReservedCash(Account account, com.bonchang.qerp.order.Order order, BigDecimal amount, String note) {
        BigDecimal normalized = amount.setScale(6, RoundingMode.HALF_UP);
        if (normalized.compareTo(ZERO) <= 0) {
            return;
        }
        CashBalance balance = lockCashBalance(account.getId());
        balance.setReservedCash(balance.getReservedCash().subtract(normalized).max(ZERO).setScale(6, RoundingMode.HALF_UP));
        balance.setAvailableCash(balance.getAvailableCash().add(normalized).setScale(6, RoundingMode.HALF_UP));
        balance.setUpdatedAt(LocalDateTime.now());
        persistLedger(account, order, CashLedgerEntryType.RELEASE_BUY_RESERVATION, normalized, balance, note);
        order.setReservedCashAmount(order.getReservedCashAmount().subtract(normalized).max(ZERO).setScale(6, RoundingMode.HALF_UP));
    }

    @Transactional
    public void settleSellProceeds(Account account, com.bonchang.qerp.order.Order order, BigDecimal amount, String note) {
        BigDecimal normalized = amount.setScale(6, RoundingMode.HALF_UP);
        if (normalized.compareTo(ZERO) <= 0) {
            return;
        }
        CashBalance balance = lockCashBalance(account.getId());
        balance.setAvailableCash(balance.getAvailableCash().add(normalized).setScale(6, RoundingMode.HALF_UP));
        balance.setUpdatedAt(LocalDateTime.now());
        persistLedger(account, order, CashLedgerEntryType.SELL_PROCEEDS, normalized, balance, note);
    }

    private void persistLedger(
            Account account,
            com.bonchang.qerp.order.Order order,
            CashLedgerEntryType entryType,
            BigDecimal amount,
            CashBalance balance,
            String note
    ) {
        CashLedgerEntry entry = new CashLedgerEntry();
        entry.setAccount(account);
        entry.setOrder(order);
        entry.setEntryType(entryType);
        entry.setAmount(amount.setScale(6, RoundingMode.HALF_UP));
        entry.setAvailableCashAfter(balance.getAvailableCash());
        entry.setReservedCashAfter(balance.getReservedCash());
        entry.setNote(note);
        entry.setCreatedAt(LocalDateTime.now());
        cashLedgerEntryRepository.save(entry);
    }
}
