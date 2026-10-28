package com.gamba.backend.service;

import com.gamba.backend.dto.piggies.PiggiesGameResponse;
import com.gamba.backend.model.PiggiesGame;
import com.gamba.backend.model.PiggiesGameStatus;
import com.gamba.backend.model.TransactionType;
import com.gamba.backend.model.User;
import com.gamba.backend.model.Wallet;
import com.gamba.backend.repository.PiggiesGameRepository;
import com.gamba.backend.repository.UserRepository;
import com.gamba.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PiggiesService {

    public static final double[] CHANCES = {0.5, 0.4, 0.3};
    public static final BigDecimal[] MULTIPLIERS = multipliers();

    private final PiggiesGameRepository piggiesGameRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final GamblingLimitService gamblingLimitService;
    private final WalletTransactionService walletTransactionService;
    private final SecureRandom secureRandom = new SecureRandom();

    private static BigDecimal[] multipliers() {
        BigDecimal[] result = new BigDecimal[CHANCES.length];
        double total = 1;
        for (int i = 0; i < CHANCES.length; i++) {
            total /= CHANCES[i];
            result[i] = BigDecimal.valueOf(Math.round(total * 100), 2);
        }
        return result;
    }

    @Transactional
    public PiggiesGameResponse start(UUID userId, BigDecimal betAmount) {
        if (piggiesGameRepository.findByUserIdAndStatus(userId, PiggiesGameStatus.IN_PROGRESS).isPresent()) {
            throw new IllegalArgumentException("You already have a round in progress");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        gamblingLimitService.checkCanBet(user, betAmount);

        Wallet wallet = user.getWallet();
        if (wallet == null || wallet.getBalance().compareTo(betAmount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        wallet.setBalance(wallet.getBalance().subtract(betAmount));
        walletRepository.save(wallet);

        PiggiesGame game = PiggiesGame.builder()
                .user(user)
                .betAmount(betAmount)
                .build();

        walletTransactionService.record(
                user,
                betAmount.negate(),
                wallet.getBalance(),
                TransactionType.BET,
                "3 Piggies & Wolf Bet",
                null
        );

        return blow(game, wallet);
    }

    @Transactional
    public PiggiesGameResponse blowNext(UUID userId) {
        PiggiesGame game = active(userId);
        return blow(game, game.getUser().getWallet());
    }

    @Transactional
    public PiggiesGameResponse cashout(UUID userId) {
        PiggiesGame game = active(userId);
        Wallet wallet = game.getUser().getWallet();
        payOut(game, wallet, PiggiesGameStatus.CASHED_OUT);
        return buildResponse(game, wallet.getBalance(), null);
    }

    @Transactional(readOnly = true)
    public PiggiesGameResponse getActiveGame(UUID userId) {
        return piggiesGameRepository.findByUserIdAndStatus(userId, PiggiesGameStatus.IN_PROGRESS)
                .map(g -> buildResponse(g, g.getUser().getWallet().getBalance(), null))
                .orElse(null);
    }

    private PiggiesGame active(UUID userId) {
        return piggiesGameRepository.findByUserIdAndStatus(userId, PiggiesGameStatus.IN_PROGRESS)
                .orElseThrow(() -> new IllegalArgumentException("No round in progress"));
    }

    private PiggiesGameResponse blow(PiggiesGame game, Wallet wallet) {
        int house = game.getHousesDown();
        boolean fell = secureRandom.nextDouble() < CHANCES[house];
        if (!fell) {
            game.setStatus(PiggiesGameStatus.LOST);
            game.setPayout(BigDecimal.ZERO);
        } else {
            game.setHousesDown(house + 1);
            if (game.getHousesDown() == CHANCES.length) {
                payOut(game, wallet, PiggiesGameStatus.WON);
            }
        }
        piggiesGameRepository.save(game);
        return buildResponse(game, wallet.getBalance(), fell);
    }

    private void payOut(PiggiesGame game, Wallet wallet, PiggiesGameStatus status) {
        BigDecimal payout = game.getBetAmount()
                .multiply(MULTIPLIERS[game.getHousesDown() - 1])
                .setScale(2, RoundingMode.HALF_UP);
        game.setPayout(payout);
        game.setStatus(status);
        wallet.setBalance(wallet.getBalance().add(payout));
        walletRepository.save(wallet);
        piggiesGameRepository.save(game);

        walletTransactionService.record(
                game.getUser(),
                payout,
                wallet.getBalance(),
                TransactionType.WIN,
                "3 Piggies & Wolf Win (" + status.name() + " - " + MULTIPLIERS[game.getHousesDown() - 1] + "x)",
                game.getId()
        );
    }

    private PiggiesGameResponse buildResponse(PiggiesGame game, BigDecimal balance, Boolean fell) {
        return PiggiesGameResponse.builder()
                .gameId(game.getId())
                .betAmount(game.getBetAmount())
                .housesDown(game.getHousesDown())
                .fell(fell)
                .status(game.getStatus())
                .payout(game.getPayout())
                .balance(balance)
                .build();
    }
}