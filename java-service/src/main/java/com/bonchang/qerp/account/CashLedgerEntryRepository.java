package com.bonchang.qerp.account;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashLedgerEntryRepository extends JpaRepository<CashLedgerEntry, Long> {

    List<CashLedgerEntry> findByAccountIdOrderByCreatedAtDescIdDesc(Long accountId, Pageable pageable);
}
