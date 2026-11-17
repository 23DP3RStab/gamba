package com.gamba.backend.service;

import com.gamba.backend.model.CrashBet;
import com.gamba.backend.model.CrashBetStatus;
import com.gamba.backend.model.CrashRound;
import com.gamba.backend.model.CrashRoundStatus;
import com.gamba.backend.model.TransactionType;
import com.gamba.backend.model.User;
import com.gamba.backend.model.Wallet;
import com.gamba.backend.repository.CrashBetRepository;
import com.gamba.backend.repository.CrashRoundRepository;
import com.gamba.backend.repository.UserRepository;
import com.gamba.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// The database side of Crash: rounds, bets and the money. CrashEngine decides when each
// of these happens; this only records it.
@Service
@RequiredArgsConstructor
public class CrashService {

    private final CrashRoundRepository crashRoundRepository;
    private final CrashBetRepository crashBetRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletLock walletLock;
    private final GamblingLimitService gamblingLimitService;
    private final WalletTransactionService walletTransactionService;

    public record PlacedBet(UUID betId, String username, BigDecimal balance) {
    }

    public record CashedOut(BigDecimal payout, BigDecimal balance) {
    }

    @Transactional
    public UUID newRound(BigDecimal crashPoint) {
        return crashRoundRepository.save(CrashRound.builder().crashPoint(crashPoint).build()).getId();
    }

    @Transactional
    public void startRound(UUID roundId, Instant at) {
        CrashRound round = crashRoundRepository.findById(roundId).orElseThrow();
        round.setStatus(CrashRoundStatus.RUNNING);
        round.setStartedAt(at);
    }

    // Takes the bet off the player's balance
    @Transactional
    public PlacedBet placeBet(UUID userId, UUID roundId, BigDecimal amount, BigDecimal autoCashout) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Wallet wallet = walletLock.lock(user);
        gamblingLimitService.checkCanBet(user, amount);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        CrashBet bet = crashBetRepository.save(CrashBet.builder()
                .round(crashRoundRepository.getReferenceById(roundId))
                .user(user)
                .betAmount(amount)
                .autoCashout(autoCashout)
                .build());
        walletTransactionService.record(user, amount.negate(), wallet.getBalance(), TransactionType.BET,
                autoCashout == null ? "Crash Bet" : "Crash Bet (auto cash-out " + autoCashout + "x)", bet.getId());
        return new PlacedBet(bet.getId(), user.getUsername(), wallet.getBalance());
    }

    // Pays the bet out at `multiplier`
    @Transactional
    public CashedOut cashOut(UUID betId, BigDecimal multiplier) {
        CrashBet bet = crashBetRepository.findById(betId).orElseThrow();
        BigDecimal payout = CrashRules.payout(bet.getBetAmount(), multiplier);
        Wallet wallet = walletLock.lock(bet.getUser());
        wallet.setBalance(wallet.getBalance().add(payout));
        walletRepository.save(wallet);

        bet.setCashoutMultiplier(multiplier);
        bet.setPayout(payout);
        bet.setStatus(CrashBetStatus.CASHED_OUT);
        walletTransactionService.record(bet.getUser(), payout, wallet.getBalance(), TransactionType.WIN,
                "Crash Cashout (" + multiplier + "x)", bet.getId());
        return new CashedOut(payout, wallet.getBalance());
    }

    @Transactional
    public void crash(UUID roundId, Instant at) {
        CrashRound round = crashRoundRepository.findById(roundId).orElseThrow();
        round.setStatus(CrashRoundStatus.CRASHED);
        round.setCrashedAt(at);
        crashBetRepository.loseRemaining(roundId, CrashBetStatus.IN_PROGRESS, CrashBetStatus.LOST);
    }

    // Rounds a restart cut short are cancelled and their open bets paid back
    @Transactional
    public void refundUnfinished() {
        for (CrashRound round : crashRoundRepository.findByStatusIn(
                List.of(CrashRoundStatus.BETTING, CrashRoundStatus.RUNNING))) {
            for (CrashBet bet : crashBetRepository.findByRoundIdAndStatus(round.getId(), CrashBetStatus.IN_PROGRESS)) {
                Wallet wallet = walletLock.lock(bet.getUser());
                wallet.setBalance(wallet.getBalance().add(bet.getBetAmount()));
                walletRepository.save(wallet);
                bet.setPayout(bet.getBetAmount());
                bet.setStatus(CrashBetStatus.REFUNDED);
                walletTransactionService.record(bet.getUser(), bet.getBetAmount(), wallet.getBalance(),
                        TransactionType.REFUND, "Crash Refund (round cut short by a restart)", bet.getId());
            }
            round.setStatus(CrashRoundStatus.CANCELLED);
        }
    }

    // The latest crash points, newest first
    @Transactional(readOnly = true)
    public List<BigDecimal> history() {
        return crashRoundRepository.findTop20ByStatusOrderByCreatedAtDesc(CrashRoundStatus.CRASHED).stream()
                .map(CrashRound::getCrashPoint)
                .toList();
    }
}
