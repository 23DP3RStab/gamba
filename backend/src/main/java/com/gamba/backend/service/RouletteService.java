package com.gamba.backend.service;

import com.gamba.backend.dto.roulette.SpinResponse;
import com.gamba.backend.model.RouletteSpin;
import com.gamba.backend.model.User;
import com.gamba.backend.model.Wallet;
import com.gamba.backend.repository.RouletteSpinRepository;
import com.gamba.backend.repository.UserRepository;
import com.gamba.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// Roulette on the real balance: the server picks the number, the browser only animates the wheel
@Service
@RequiredArgsConstructor
public class RouletteService {

    private static final BigDecimal MIN_BET = new BigDecimal("0.10");

    private final RouletteSpinRepository rouletteSpinRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public SpinResponse spin(UUID userId, Map<String, BigDecimal> bets) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> bet : bets.entrySet()) {
            if (!RouletteBets.BETS.containsKey(bet.getKey())) {
                throw new IllegalArgumentException("Unknown bet: " + bet.getKey());
            }
            BigDecimal amount = bet.getValue();
            if (amount == null || amount.compareTo(MIN_BET) < 0 || amount.stripTrailingZeros().scale() > 2) {
                throw new IllegalArgumentException("Each bet must be at least 0.10 with at most 2 decimals");
            }
            total = total.add(amount);
        }
        total = total.setScale(2);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Wallet wallet = user.getWallet();
        if (wallet == null || wallet.getBalance().compareTo(total) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        int number = secureRandom.nextInt(37);
        BigDecimal payout = bets.entrySet().stream()
                .map(bet -> RouletteBets.returnFor(bet.getKey(), bet.getValue(), number))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);

        wallet.setBalance(wallet.getBalance().subtract(total).add(payout));
        walletRepository.save(wallet);

        rouletteSpinRepository.save(RouletteSpin.builder()
                .user(user)
                .totalBet(total)
                .payout(payout)
                .winningNumber(number)
                .bets(bets.entrySet().stream()
                        .map(bet -> bet.getKey() + ":" + bet.getValue().toPlainString())
                        .collect(Collectors.joining(",")))
                .build());

        return SpinResponse.builder()
                .number(number)
                .totalBet(total)
                .payout(payout)
                .balance(wallet.getBalance())
                .build();
    }
}
