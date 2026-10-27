package com.gamba.backend.wallet;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WithdrawalLedger {
    public record Withdrawal(UUID id, UUID walletId, BigDecimal amount, String recipient, String currency,
                             String status, String providerStatus, String batchId, String itemId,
                             String transactionId, String errorCode, Instant createdAt, int submitAttempts) {}
    public record Claim(Withdrawal withdrawal, boolean claimed) {}
    public record View(String id, String amount, String recipient, String currency, String status,
                       String providerStatus, String batchId, String transactionId, String errorCode,
                       String balance, String reserved, String message) {}

    private final SandboxWalletRepository wallets;
    private final SandboxWithdrawalRepository withdrawals;

    public WithdrawalLedger(SandboxWalletRepository wallets, SandboxWithdrawalRepository withdrawals) {
        this.wallets = wallets; this.withdrawals = withdrawals;
    }

    @Transactional
    public Withdrawal reserve(UUID walletId, UUID requestId, BigDecimal amount, String email) {
        SandboxWallet wallet = lock(walletId);
        String recipient = email.trim().toLowerCase(Locale.ROOT);
        SandboxWithdrawal prior = withdrawals.findById(requestId).orElse(null);
        if (prior != null) {
            if (!prior.getWalletId().equals(walletId) || prior.getAmount().compareTo(amount) != 0
                    || !prior.getRecipient().equals(recipient)) {
                throw new PaymentException(HttpStatus.CONFLICT, "REQUEST_CONFLICT",
                        "Šis pieprasījuma ID jau izmantots citai izmaksai.");
            }
            return snapshot(prior);
        }
        if (amount.signum() <= 0 || amount.compareTo(new BigDecimal("1000.00")) > 0 || amount.scale() > 2) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Nederīga izmaksas summa.");
        }
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "INSUFFICIENT_BALANCE", "Nepietiek līdzekļu.");
        }
        wallet.reserve(amount);
        SandboxWithdrawal withdrawal = withdrawals.save(new SandboxWithdrawal(requestId, walletId, amount.setScale(2), recipient));
        withdrawals.flush();
        return snapshot(withdrawal);
    }

    @Transactional(readOnly = true)
    public Withdrawal find(UUID walletId, UUID id) { return snapshot(owned(walletId, id)); }

    @Transactional(readOnly = true)
    public BigDecimal reserved(UUID walletId) { return withdrawals.reserved(walletId).setScale(2); }

    @Transactional(readOnly = true)
    public List<View> recent(UUID walletId) {
        SandboxWallet wallet = wallets.findById(walletId).orElseThrow();
        String held = reserved(walletId).toPlainString();
        // Always expose unresolved payouts, even after they fall outside the recent history window.
        LinkedHashMap<UUID, SandboxWithdrawal> rows = new LinkedHashMap<>();
        withdrawals.findByWalletIdAndStatusInOrderByCreatedAtDesc(walletId, List.of("REQUESTED", "PROCESSING"))
                .forEach(w -> rows.put(w.getId(), w));
        withdrawals.findTop10ByWalletIdOrderByCreatedAtDesc(walletId).forEach(w -> rows.putIfAbsent(w.getId(), w));
        return rows.values().stream()
                .map(w -> view(w, wallet.getBalance().toPlainString(), held)).toList();
    }

    @Transactional(readOnly = true)
    public View view(UUID walletId, UUID id) {
        return view(owned(walletId, id), wallets.findById(walletId).orElseThrow().getBalance().toPlainString(),
                reserved(walletId).toPlainString());
    }

    @Transactional
    public Claim claimSubmission(UUID walletId, UUID id) {
        lock(walletId);
        SandboxWithdrawal w = owned(walletId, id);
        if (w.getPayoutBatchId() != null || w.terminalFailure() || w.getStatus().equals("SUCCESS")) {
            return new Claim(snapshot(w), false);
        }
        Instant now = Instant.now();
        if (w.getSubmitLeaseUntil() != null && w.getSubmitLeaseUntil().isAfter(now)) {
            return new Claim(snapshot(w), false);
        }
        // PayPal's duplicate batch protection lasts 30 days. Do not recreate an uncertain old payout.
        if (w.getSubmitAttempts() > 0 && w.getCreatedAt().isBefore(now.minus(Duration.ofDays(29)))) {
            throw new PaymentException(HttpStatus.CONFLICT, "PAYOUT_REVIEW_REQUIRED",
                    "Vecas izmaksas statuss jāpārbauda PayPal vēsturē. Jauna izmaksa nav nosūtīta.");
        }
        w.claimSubmission(now.plusSeconds(90));
        return new Claim(snapshot(w), true);
    }

    @Transactional
    public void attachBatch(UUID walletId, UUID id, String batchId) {
        lock(walletId); SandboxWithdrawal w = owned(walletId, id);
        if (w.getPayoutBatchId() != null && !w.getPayoutBatchId().equals(batchId)) throw mismatch();
        if (w.terminalFailure()) throw mismatch();
        if (w.getPayoutBatchId() == null) w.setBatch(batchId);
        withdrawals.flush();
    }

    @Transactional
    public void submissionFailed(UUID walletId, UUID id, int attempt, String code, boolean definite) {
        SandboxWallet wallet = lock(walletId); SandboxWithdrawal w = owned(walletId, id);
        if (w.getPayoutBatchId() != null || w.terminalFailure()) return;
        if (w.getSubmitAttempts() != attempt) return;
        if (definite && attempt == 1) {
            if (!w.isFundsReleased()) wallet.add(w.getAmount());
            w.release("FAILED", "REJECTED", code);
        } else { w.setError(code); }
    }

    @Transactional
    public void apply(UUID walletId, UUID id, PayoutGateway.Result result) {
        SandboxWallet wallet = lock(walletId); SandboxWithdrawal w = owned(walletId, id);
        if (w.getPayoutBatchId() == null || !w.getPayoutBatchId().equals(result.batchId())) throw mismatch();
        if (w.getPayoutItemId() != null && result.itemId() != null && !w.getPayoutItemId().equals(result.itemId())) throw mismatch();
        // Returned/failed funds are released exactly once. Stale responses cannot undo a terminal state.
        if (w.terminalFailure()) return;
        String state = result.status();
        if (List.of("FAILED", "DENIED", "CANCELED", "RETURNED", "REFUNDED", "REVERSED").contains(state)) {
            if (w.getStatus().equals("SUCCESS") && !List.of("RETURNED", "REFUNDED", "REVERSED").contains(state)) return;
            if (!w.isFundsReleased()) wallet.add(w.getAmount());
            w.update(w.getStatus(), state, result.itemId(), result.transactionId());
            w.release(List.of("RETURNED", "REFUNDED", "REVERSED").contains(state) ? "RETURNED" : "FAILED", state, null);
        } else if (state.equals("SUCCESS")) {
            w.update("SUCCESS", state, result.itemId(), result.transactionId());
        } else if (!w.getStatus().equals("SUCCESS")) {
            w.update("PROCESSING", state, result.itemId(), result.transactionId());
        }
        withdrawals.flush();
    }

    private SandboxWallet lock(UUID id) {
        return wallets.lockById(id).orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "Maks nav atrasts."));
    }
    private SandboxWithdrawal owned(UUID walletId, UUID id) {
        return withdrawals.findById(id).filter(w -> w.getWalletId().equals(walletId)).orElseThrow(() ->
                new PaymentException(HttpStatus.NOT_FOUND, "WITHDRAWAL_NOT_FOUND", "Izmaksas pieprasījums nav atrasts."));
    }
    private PaymentException mismatch() {
        return new PaymentException(HttpStatus.BAD_GATEWAY, "PAYOUT_MISMATCH", "PayPal izmaksas dati nesakrīt.");
    }
    private Withdrawal snapshot(SandboxWithdrawal w) {
        return new Withdrawal(w.getId(), w.getWalletId(), w.getAmount(), w.getRecipient(), w.getCurrency(),
                w.getStatus(), w.getProviderStatus(), w.getPayoutBatchId(), w.getPayoutItemId(),
                w.getTransactionId(), w.getErrorCode(), w.getCreatedAt(), w.getSubmitAttempts());
    }
    private View view(SandboxWithdrawal w, String balance, String reserved) {
        String message = switch (w.getStatus()) {
            case "SUCCESS" -> "PayPal Sandbox izmaksa pabeigta.";
            case "FAILED" -> "Izmaksa noraidīta. Rezervētā summa atgriezta pieejamajā bilancē.";
            case "RETURNED" -> "PayPal atgrieza izmaksu. Summa atjaunota pieejamajā bilancē.";
            default -> "Izmaksa tiek pārbaudīta vai apstrādāta. Summa ir rezervēta.";
        };
        if ("PAYOUT_ACCESS_DENIED".equals(w.getErrorCode())) message += " Sandbox lietotnei vajadzīga PayPal Payouts piekļuve.";
        if ("PAYOUT_INSUFFICIENT_FUNDS".equals(w.getErrorCode())) message += " Business Sandbox kontā nepietiek līdzekļu izmaksai un komisijai.";
        return new View(w.getId().toString(), w.getAmount().toPlainString(), w.getRecipient(), w.getCurrency(),
                w.getStatus(), w.getProviderStatus(), w.getPayoutBatchId(), w.getTransactionId(), w.getErrorCode(), balance, reserved, message);
    }
}
