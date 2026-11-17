package com.gamba.backend.service;

import com.gamba.backend.model.User;
import com.gamba.backend.model.Wallet;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

// Locks a player's wallet row for the rest of the transaction (SELECT … FOR UPDATE), so two
// balance changes at the same moment (a bet, a Crash payout, an admin adjustment, a PayPal deposit)
// wait for each other instead of one overwriting the other. Must run inside a transaction.
@Component
public class WalletLock {

    @PersistenceContext
    private EntityManager entityManager;

    public Wallet lock(User user) {
        Wallet wallet = user.getWallet();
        if (wallet == null) {
            throw new IllegalArgumentException("Wallet not found");
        }
        // The wallet usually came along with the user, unlocked; re-read it under the lock
        entityManager.refresh(wallet, LockModeType.PESSIMISTIC_WRITE);
        return wallet;
    }
}
