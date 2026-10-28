package com.gamba.backend.wallet;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Local Sandbox identity until the shared login system is integrated. */
@Entity
@Table(name = "sandbox_wallets")
public class SandboxWallet {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true, length = 64)
    private String accessKeyHash;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;
    @Column(nullable = false)
    private Instant createdAt;

    protected SandboxWallet() {}

    SandboxWallet(String accessKeyHash) {
        this.id = UUID.randomUUID();
        this.accessKeyHash = accessKeyHash;
        this.balance = new BigDecimal("0.00");
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public BigDecimal getBalance() { return balance; }
    void add(BigDecimal amount) { balance = balance.add(amount); }
    void reserve(BigDecimal amount) { balance = balance.subtract(amount); }
}
