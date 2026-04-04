package com.bonchang.qerp.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_code", nullable = false, unique = true, length = 64)
    private String accountCode;

    @Column(name = "owner_name", nullable = false, length = 128)
    private String ownerName;

    @Column(name = "base_currency", nullable = false, length = 16)
    private String baseCurrency;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
