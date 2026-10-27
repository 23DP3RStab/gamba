package com.gamba.backend.wallet;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sandbox_deposits", indexes = @Index(name = "sandbox_deposit_wallet_idx", columnList = "wallet_id"))
public class SandboxDeposit {
    @Id
    private UUID id;
    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(unique = true, length = 36)
    private String paypalOrderId;
    @Column(unique = true, length = 64)
    private String captureId;
    @Column(nullable = false, length = 36)
    private String captureRequestId;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant creditedAt;

    protected SandboxDeposit() {}

    SandboxDeposit(UUID id, UUID walletId, BigDecimal amount) {
        this.id = id;
        this.walletId = walletId;
        this.amount = amount;
        this.currency = "EUR";
        this.captureRequestId = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWalletId() { return walletId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getPaypalOrderId() { return paypalOrderId; }
    public String getCaptureId() { return captureId; }
    public String getCaptureRequestId() { return captureRequestId; }
    public Instant getCreditedAt() { return creditedAt; }
    void attachOrder(String orderId) { paypalOrderId = orderId; }
    void markCredited(String id) { captureId = id; creditedAt = Instant.now(); }
}
