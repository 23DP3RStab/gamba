package com.gamba.backend.wallet;

import com.gamba.backend.model.Wallet;
import com.gamba.backend.repository.WalletRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Connects existing PayPal payment records to the same balance used by games. */
@Service
public class AccountBalance {
    @PersistenceContext private EntityManager em;

    private UUID userId(UUID paymentWalletId) {
        String key = em.createQuery("select w.accessKeyHash from SandboxWallet w where w.id = :id", String.class)
                .setParameter("id", paymentWalletId).getResultStream().findFirst()
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "Wallet not found."));
        if (!key.startsWith("user:")) throw new PaymentException(
                HttpStatus.CONFLICT, "OLD_SANDBOX_WALLET", "Wallet needs a linked account.");
        try { return UUID.fromString(key.substring(5)); }
        catch (IllegalArgumentException ex) { throw new PaymentException(
                HttpStatus.CONFLICT, "INVALID_WALLET", "Invalid wallet account."); }
    }

    @Transactional(readOnly = true)
    public BigDecimal balance(UUID paymentWalletId) {
        return em.createQuery("select w.balance from Wallet w where w.user.id = :userId", BigDecimal.class)
                .setParameter("userId", userId(paymentWalletId)).getResultStream().findFirst()
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "ACCOUNT_WALLET_NOT_FOUND", "Account wallet not found."));
    }

    /** Must be invoked inside the caller's transaction. */
    public Wallet locked(UUID paymentWalletId) {
        return em.createQuery("select w from Wallet w where w.user.id = :userId", Wallet.class)
                .setParameter("userId", userId(paymentWalletId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream().findFirst().orElseThrow(() -> new PaymentException(
                        HttpStatus.NOT_FOUND, "ACCOUNT_WALLET_NOT_FOUND", "Account wallet not found."));
    }
}
