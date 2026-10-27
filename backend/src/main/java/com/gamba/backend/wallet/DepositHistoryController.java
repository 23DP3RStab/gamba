package com.gamba.backend.wallet;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DepositHistoryController {
    private final BrowserWallet browserWallet;
    @PersistenceContext private EntityManager entityManager;
    public DepositHistoryController(BrowserWallet browserWallet) { this.browserWallet = browserWallet; }
    public record Entry(String id, String amount, String status, Instant date) {}

    @GetMapping("/api/paypal/deposits")
    @Transactional
    public List<Entry> deposits(HttpServletRequest request, HttpServletResponse response) {
        UUID walletId = browserWallet.resolve(request, response);
        return entityManager.createQuery(
                "select d from SandboxDeposit d where d.walletId = :walletId "
                + "and d.creditedAt is not null order by d.creditedAt desc", SandboxDeposit.class)
                .setParameter("walletId", walletId).getResultList().stream()
                .map(d -> new Entry(d.getId().toString(), d.getAmount().toPlainString(), "SUCCESS", d.getCreditedAt())).toList();
    }

    @GetMapping("/api/paypal/withdrawal-history")
    @Transactional
    public List<Entry> withdrawals(HttpServletRequest request, HttpServletResponse response) {
        UUID walletId = browserWallet.resolve(request, response);
        return entityManager.createQuery(
                "select w from SandboxWithdrawal w where w.walletId = :walletId order by w.createdAt desc",
                SandboxWithdrawal.class)
                .setParameter("walletId", walletId).getResultList().stream()
                .map(w -> new Entry(w.getId().toString(), w.getAmount().toPlainString(), w.getStatus(), w.getCreatedAt()))
                .toList();
    }

}
