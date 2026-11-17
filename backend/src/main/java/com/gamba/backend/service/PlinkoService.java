package com.gamba.backend.service;

import com.gamba.backend.dto.plinko.PlinkoDropResponse;
import com.gamba.backend.model.PlinkoDrop;
import com.gamba.backend.model.PlinkoRisk;
import com.gamba.backend.model.TransactionType;
import com.gamba.backend.model.User;
import com.gamba.backend.model.Wallet;
import com.gamba.backend.repository.PlinkoDropRepository;
import com.gamba.backend.repository.UserRepository;
import com.gamba.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.UUID;

// Plinko on the real balance: the server bounces the ball and settles the bet straight away,
// the browser only animates the path it was sent
@Service
@RequiredArgsConstructor
public class PlinkoService {

    private final PlinkoDropRepository plinkoDropRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletLock walletLock;
    private final GamblingLimitService gamblingLimitService;
    private final WalletTransactionService walletTransactionService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public PlinkoDropResponse drop(UUID userId, BigDecimal bet, PlinkoRisk risk) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Wallet wallet = walletLock.lock(user);
        gamblingLimitService.checkCanBet(user, bet);
        if (wallet.getBalance().compareTo(bet) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        StringBuilder bounces = new StringBuilder();
        for (int row = 0; row < PlinkoRules.ROWS; row++) {
            bounces.append(secureRandom.nextBoolean() ? 'R' : 'L');
        }
        String path = bounces.toString();
        int slot = PlinkoRules.slotOf(path);
        BigDecimal multiplier = PlinkoRules.MULTIPLIERS.get(risk).get(slot);
        BigDecimal payout = PlinkoRules.payout(bet, multiplier);

        BigDecimal balanceAfterBet = wallet.getBalance().subtract(bet);
        wallet.setBalance(balanceAfterBet.add(payout));
        walletRepository.save(wallet);

        PlinkoDrop drop = plinkoDropRepository.save(PlinkoDrop.builder()
                .user(user)
                .betAmount(bet)
                .risk(risk)
                .path(path)
                .slot(slot)
                .multiplier(multiplier)
                .payout(payout)
                .build());

        walletTransactionService.record(user, bet.negate(), balanceAfterBet, TransactionType.BET,
                "Plinko Bet (" + risk.getLabel() + " risk)", drop.getId());
        if (payout.signum() > 0) {
            walletTransactionService.record(user, payout, wallet.getBalance(), TransactionType.WIN,
                    "Plinko Win (" + multiplier.stripTrailingZeros().toPlainString() + "x)", drop.getId());
        }

        return PlinkoDropResponse.builder()
                .path(path)
                .slot(slot)
                .multiplier(multiplier)
                .payout(payout)
                .balance(wallet.getBalance())
                .build();
    }
}
