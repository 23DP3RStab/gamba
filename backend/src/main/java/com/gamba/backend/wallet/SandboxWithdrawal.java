package com.gamba.backend.wallet;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sandbox_withdrawals", indexes = @Index(name = "sandbox_withdrawal_wallet_idx", columnList = "wallet_id"))
public class SandboxWithdrawal {
    @Id private UUID id;
    @Column(name = "wallet_id", nullable = false) private UUID walletId;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 127) private String recipient;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false, length = 20) private String status;
    @Column(length = 40) private String providerStatus;
    @Column(unique = true, length = 64) private String payoutBatchId;
    @Column(unique = true, length = 64) private String payoutItemId;
    @Column(length = 64) private String transactionId;
    @Column(length = 80) private String errorCode;
    @Column(nullable = false) private Instant createdAt;
    private Instant updatedAt;
    private Instant submitLeaseUntil;
    @Column(nullable = false) private int submitAttempts;
    @Column(nullable = false) private boolean fundsReleased;

    protected SandboxWithdrawal() {}
    SandboxWithdrawal(UUID id, UUID walletId, BigDecimal amount, String recipient) {
        this.id = id; this.walletId = walletId; this.amount = amount;
        this.recipient = recipient; this.currency = "EUR"; this.status = "REQUESTED";
        this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    public UUID getId() { return id; }
    public UUID getWalletId() { return walletId; }
    public BigDecimal getAmount() { return amount; }
    public String getRecipient() { return recipient; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getProviderStatus() { return providerStatus; }
    public String getPayoutBatchId() { return payoutBatchId; }
    public String getPayoutItemId() { return payoutItemId; }
    public String getTransactionId() { return transactionId; }
    public String getErrorCode() { return errorCode; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSubmitLeaseUntil() { return submitLeaseUntil; }
    public int getSubmitAttempts() { return submitAttempts; }
    public boolean isFundsReleased() { return fundsReleased; }
    boolean terminalFailure() { return status.equals("FAILED") || status.equals("RETURNED"); }
    void claimSubmission(Instant until) { submitAttempts++; submitLeaseUntil = until; updatedAt = Instant.now(); }
    void clearLease() { submitLeaseUntil = null; updatedAt = Instant.now(); }
    void setBatch(String batchId) { payoutBatchId = batchId; status = "PROCESSING"; errorCode = null; clearLease(); }
    void setError(String code) { errorCode = code; clearLease(); }
    void update(String status, String providerStatus, String itemId, String transactionId) {
        this.status = status; this.providerStatus = providerStatus;
        if (itemId != null) this.payoutItemId = itemId;
        if (transactionId != null) this.transactionId = transactionId;
        this.errorCode = null; this.updatedAt = Instant.now();
    }
    void release(String status, String providerStatus, String code) {
        fundsReleased = true; this.status = status; this.providerStatus = providerStatus;
        this.errorCode = code; clearLease();
    }
}
