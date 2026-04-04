package com.bonchang.qerp.account;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface CashBalanceRepository extends JpaRepository<CashBalance, Long> {

    Optional<CashBalance> findByAccountId(Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select cb
            from CashBalance cb
            where cb.account.id = :accountId
            """)
    Optional<CashBalance> lockByAccountId(@Param("accountId") Long accountId);
}
