package com.gamba.backend.wallet;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class WithdrawalService {
    private final WithdrawalLedger ledger;
    private final PayoutGateway paypal;
    public WithdrawalService(WithdrawalLedger ledger, PayoutGateway paypal) { this.ledger = ledger; this.paypal = paypal; }

    public WithdrawalLedger.View request(UUID walletId, UUID requestId, BigDecimal amount, String recipient) {
        WithdrawalLedger.Withdrawal w = ledger.reserve(walletId, requestId, amount, recipient);
        return process(walletId, w.id(), false);
    }
    public WithdrawalLedger.View check(UUID walletId, UUID id) { return process(walletId, id, true); }

    private WithdrawalLedger.View process(UUID walletId, UUID id, boolean refresh) {
        WithdrawalLedger.Withdrawal w = ledger.find(walletId, id);
        if (w.status().equals("FAILED") || w.status().equals("RETURNED") || (!refresh && w.status().equals("SUCCESS")))
            return ledger.view(walletId, id);
        if (w.batchId() == null) {
            WithdrawalLedger.Claim claim = ledger.claimSubmission(walletId, id);
            if (!claim.claimed()) return ledger.view(walletId, id);
            w = claim.withdrawal();
            try {
                String batchId = paypal.submit(w);
                ledger.attachBatch(walletId, id, batchId);
            } catch (PayoutRejectedException ex) {
                ledger.submissionFailed(walletId, id, w.submitAttempts(), ex.getCode(), true);
                return ledger.view(walletId, id);
            } catch (PaymentException ex) {
                ledger.submissionFailed(walletId, id, w.submitAttempts(), ex.getCode(), false);
                throw ex;
            }
            w = ledger.find(walletId, id);
        }
        // Only the verified individual payout status can finalize or refund a reservation.
        ledger.apply(walletId, id, paypal.check(w));
        return ledger.view(walletId, id);
    }
}
