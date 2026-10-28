package com.gamba.backend.service;

import com.gamba.backend.dto.mines.MinesGameResponse;
import com.gamba.backend.dto.mines.StartMinesRequest;
import com.gamba.backend.model.MinesGame;
import com.gamba.backend.model.MinesGameStatus;
import com.gamba.backend.model.TransactionType;
import com.gamba.backend.model.User;
import com.gamba.backend.model.Wallet;
import com.gamba.backend.repository.MinesGameRepository;
import com.gamba.backend.repository.UserRepository;
import com.gamba.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MinesGameService {

    private static final int TOTAL_TILES = 25;
    private static final double RTP = 0.99;

    private final MinesGameRepository minesGameRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final GamblingLimitService gamblingLimitService;
    private final WalletTransactionService walletTransactionService;
    private final WalletLock walletLock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public MinesGameResponse startGame(UUID userId, StartMinesRequest request) {
        if (minesGameRepository.findByUserIdAndStatus(userId, MinesGameStatus.IN_PROGRESS).isPresent()) {
            throw new IllegalArgumentException("You already have an active game in progress");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Wallet wallet = walletLock.lock(user);
        gamblingLimitService.checkCanBet(user, request.getBetAmount());
        if (wallet.getBalance().compareTo(request.getBetAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(request.getBetAmount()));
        walletRepository.save(wallet);

        List<Integer> tilePool = new ArrayList<>(TOTAL_TILES);
        for (int i = 0; i < TOTAL_TILES; i++) {
            tilePool.add(i);
        }
        Collections.shuffle(tilePool, secureRandom);
        List<Integer> mines = tilePool.subList(0, request.getMineCount());
        String minePositionsStr = mines.stream().map(String::valueOf).collect(Collectors.joining(","));

        MinesGame game = MinesGame.builder()
                .user(user)
                .betAmount(request.getBetAmount())
                .mineCount(request.getMineCount())
                .minePositions(minePositionsStr)
                .revealedTiles("")
                .currentMultiplier(BigDecimal.ONE)
                .payout(BigDecimal.ZERO)
                .status(MinesGameStatus.IN_PROGRESS)
                .build();

        MinesGame saved = minesGameRepository.save(game);

        walletTransactionService.record(
                user,
                request.getBetAmount().negate(),
                wallet.getBalance(),
                TransactionType.BET,
                "Mines Bet (" + request.getMineCount() + " mines)",
                saved.getId()
        );

        return buildResponse(saved, wallet.getBalance(), false);
    }

    @Transactional
    public MinesGameResponse revealTile(UUID userId, int tileIndex) {
        MinesGame game = minesGameRepository.findByUserIdAndStatus(userId, MinesGameStatus.IN_PROGRESS)
                .orElseThrow(() -> new IllegalArgumentException("No active game in progress"));

        List<Integer> revealed = parseTileList(game.getRevealedTiles());
        if (revealed.contains(tileIndex)) {
            throw new IllegalArgumentException("Tile already revealed");
        }

        List<Integer> mines = parseTileList(game.getMinePositions());
        Wallet wallet = walletLock.lock(game.getUser());

        if (mines.contains(tileIndex)) {
            revealed.add(tileIndex);
            game.setRevealedTiles(toCsv(revealed));
            game.setStatus(MinesGameStatus.BUSTED);
            game.setCurrentMultiplier(BigDecimal.ZERO);
            game.setPayout(BigDecimal.ZERO);
            minesGameRepository.save(game);

            return buildResponse(game, wallet.getBalance(), true);
        }

        revealed.add(tileIndex);
        game.setRevealedTiles(toCsv(revealed));

        BigDecimal newMultiplier = calculateMultiplier(TOTAL_TILES, game.getMineCount(), revealed.size());
        game.setCurrentMultiplier(newMultiplier);

        int totalSafeTiles = TOTAL_TILES - game.getMineCount();
        if (revealed.size() == totalSafeTiles) {
            return executeCashout(game, wallet);
        }

        minesGameRepository.save(game);
        return buildResponse(game, wallet.getBalance(), false);
    }

    @Transactional
    public MinesGameResponse cashout(UUID userId) {
        MinesGame game = minesGameRepository.findByUserIdAndStatus(userId, MinesGameStatus.IN_PROGRESS)
                .orElseThrow(() -> new IllegalArgumentException("No active game in progress"));

        List<Integer> revealed = parseTileList(game.getRevealedTiles());
        if (revealed.isEmpty()) {
            throw new IllegalArgumentException("Must reveal at least one safe tile before cashing out");
        }

        Wallet wallet = walletLock.lock(game.getUser());
        return executeCashout(game, wallet);
    }

    @Transactional(readOnly = true)
    public MinesGameResponse getActiveGame(UUID userId) {
        return minesGameRepository.findByUserIdAndStatus(userId, MinesGameStatus.IN_PROGRESS)
                .map(g -> buildResponse(g, g.getUser().getWallet().getBalance(), false))
                .orElse(null);
    }

    private MinesGameResponse executeCashout(MinesGame game, Wallet wallet) {
        BigDecimal payout = game.getBetAmount().multiply(game.getCurrentMultiplier()).setScale(2, RoundingMode.FLOOR);
        game.setPayout(payout);
        game.setStatus(MinesGameStatus.CASHED_OUT);

        wallet.setBalance(wallet.getBalance().add(payout));
        walletRepository.save(wallet);
        minesGameRepository.save(game);

        walletTransactionService.record(
                game.getUser(),
                payout,
                wallet.getBalance(),
                TransactionType.WIN,
                "Mines Cashout (" + game.getCurrentMultiplier() + "x)",
                game.getId()
        );

        return buildResponse(game, wallet.getBalance(), true);
    }

    private BigDecimal calculateMultiplier(int total, int mines, int safeRevealed) {
        if (safeRevealed == 0) return BigDecimal.ONE;

        double multiplier = RTP;
        for (int i = 0; i < safeRevealed; i++) {
            double prob = (double) (total - mines - i) / (total - i);
            multiplier *= (1.0 / prob);
        }

        return BigDecimal.valueOf(multiplier).setScale(4, RoundingMode.HALF_UP);
    }

    private MinesGameResponse buildResponse(MinesGame game, BigDecimal balance, boolean revealMines) {
        List<Integer> revealed = parseTileList(game.getRevealedTiles());
        List<Integer> mines = revealMines ? parseTileList(game.getMinePositions()) : null;

        BigDecimal nextMultiplier = null;
        if (game.getStatus() == MinesGameStatus.IN_PROGRESS) {
            nextMultiplier = calculateMultiplier(TOTAL_TILES, game.getMineCount(), revealed.size() + 1);
        }

        return MinesGameResponse.builder()
                .gameId(game.getId())
                .betAmount(game.getBetAmount())
                .mineCount(game.getMineCount())
                .revealedTiles(revealed)
                .currentMultiplier(game.getCurrentMultiplier())
                .nextMultiplier(nextMultiplier)
                .status(game.getStatus())
                .payout(game.getPayout())
                .minePositions(mines)
                .balance(balance)
                .build();
    }

    private List<Integer> parseTileList(String csv) {
        if (csv == null || csv.trim().isEmpty()) return new ArrayList<>();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    private String toCsv(List<Integer> list) {
        return list.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}