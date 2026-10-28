package com.gamba.backend.wallet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletLedger {
    public record Deposit(UUID id, UUID walletId, BigDecimal amount, String currency,
                          String orderId, String captureRequestId, String captureId, Instant creditedAt) {}
    public record Receipt(String id, String captureId, String status, String amount,
                          String currency, String balance) {}

    private final SandboxWalletRepository wallets;
    private final SandboxDepositRepository deposits;
    private final AccountBalance accountBalance;

    public WalletLedger(SandboxWalletRepository wallets, SandboxDepositRepository deposits, AccountBalance accountBalance) {
        this.wallets = wallets;
        this.deposits = deposits;
        this.accountBalance = accountBalance;
    }

    @Transactional
    public UUID resolveWallet(String accessKeyHash) {
        return wallets.findByAccessKeyHash(accessKeyHash)
                .orElseGet(() -> wallets.save(new SandboxWallet(accessKeyHash))).getId();
    }

    @Transactional(readOnly = true)
    public BigDecimal balance(UUID walletId) { return accountBalance.balance(walletId); }

    @Transactional
    public Deposit prepare(UUID walletId, UUID requestId, BigDecimal amount) {
        lock(walletId);
        SandboxDeposit deposit = deposits.findById(requestId).orElse(null);
        if (deposit == null) {
            deposit = deposits.save(new SandboxDeposit(requestId, walletId, amount.setScale(2)));
        } else if (!deposit.getWalletId().equals(walletId)
                || deposit.getAmount().compareTo(amount) != 0) {
            throw new PaymentException(HttpStatus.CONFLICT, "REQUEST_CONFLICT",
                    "Pieprasījuma ID jau izmantots citam maksājumam. Pārlādē formu.");
        }
        return snapshot(deposit);
    }

    @Transactional
    public Deposit attachOrder(UUID walletId, UUID depositId, String orderId) {
        lock(walletId);
        SandboxDeposit deposit = owned(walletId, depositId);
        if (deposit.getPaypalOrderId() != null && !deposit.getPaypalOrderId().equals(orderId)) {
            throw new PaymentException(HttpStatus.CONFLICT, "ORDER_CONFLICT", "PayPal pasūtījums nesakrīt.");
        }
        deposit.attachOrder(orderId);
        deposits.flush();
        return snapshot(deposit);
    }

    @Transactional(readOnly = true)
    public Deposit findOrder(UUID walletId, String orderId) {
        return snapshot(deposits.findByPaypalOrderIdAndWalletId(orderId, walletId)
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND",
                        "Šajā testa makā pasūtījums nav atrasts.")));
    }

    @Transactional(readOnly = true)
    public Receipt receipt(UUID walletId, UUID depositId) {
        SandboxDeposit deposit = owned(walletId, depositId);
        if (deposit.getCreditedAt() == null) {
            throw new PaymentException(HttpStatus.CONFLICT, "NOT_CREDITED", "Maksājums vēl nav ieskaitīts.");
        }
        return receipt(deposit, accountBalance.balance(walletId));
    }

    /** Both updates commit together. The database row lock also protects concurrent requests. */
    @Transactional
    public Receipt credit(UUID walletId, UUID depositId, PayPalGateway.Capture verified) {
        lock(walletId);
        var wallet = accountBalance.locked(walletId);
        SandboxDeposit deposit = owned(walletId, depositId);
        if (!deposit.getPaypalOrderId().equals(verified.orderId())
                || deposit.getAmount().compareTo(verified.amount()) != 0
                || !deposit.getCurrency().equals(verified.currency())) {
            throw new PaymentException(HttpStatus.BAD_GATEWAY, "CAPTURE_MISMATCH",
                    "PayPal maksājuma dati nesakrīt ar saglabāto pasūtījumu.");
        }
        if (deposit.getCreditedAt() == null) {
            wallet.setBalance(wallet.getBalance().add(deposit.getAmount()));
            deposit.markCredited(verified.captureId());
            // Unique order/capture IDs prevent another record crediting the same provider payment.
            deposits.flush();
        } else if (!deposit.getCaptureId().equals(verified.captureId())) {
            throw new PaymentException(HttpStatus.CONFLICT, "CAPTURE_CONFLICT", "Maksājuma ID nesakrīt.");
        }
        return receipt(deposit, wallet.getBalance());
    }

    private SandboxWallet lock(UUID id) {
        return wallets.lockById(id).orElseThrow(() -> new PaymentException(
                HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "Testa maks nav atrasts."));
    }

    private SandboxWallet wallet(UUID id) {
        return wallets.findById(id).orElseThrow(() -> new PaymentException(
                HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "Testa maks nav atrasts."));
    }

    private SandboxDeposit owned(UUID walletId, UUID depositId) {
        return deposits.findById(depositId).filter(d -> d.getWalletId().equals(walletId))
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND",
                        "Šajā testa makā pasūtījums nav atrasts."));
    }

    private Deposit snapshot(SandboxDeposit d) {
        return new Deposit(d.getId(), d.getWalletId(), d.getAmount(), d.getCurrency(),
                d.getPaypalOrderId(), d.getCaptureRequestId(), d.getCaptureId(), d.getCreditedAt());
    }

    private Receipt receipt(SandboxDeposit d, BigDecimal balance) {
        return new Receipt(d.getPaypalOrderId(), d.getCaptureId(), "COMPLETED",
                d.getAmount().toPlainString(), d.getCurrency(), balance.toPlainString());
    }
}
