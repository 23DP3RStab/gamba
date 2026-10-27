package com.gamba.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

// Totals for one game over a player's finished rounds
@Data
@Builder
public class GameStats {
    private long played;
    private long wins;
    private BigDecimal wagered;
    private BigDecimal won;
    private BigDecimal net;
    private BigDecimal biggestWin;

    public record Round(BigDecimal bet, BigDecimal payout) {
    }

    // A round is a win when it paid back more than was bet
    public static GameStats of(List<Round> rounds) {
        BigDecimal wagered = BigDecimal.ZERO;
        BigDecimal won = BigDecimal.ZERO;
        BigDecimal biggestWin = BigDecimal.ZERO;
        long wins = 0;
        for (Round round : rounds) {
            wagered = wagered.add(round.bet());
            won = won.add(round.payout());
            BigDecimal profit = round.payout().subtract(round.bet());
            if (profit.signum() > 0) {
                wins++;
                biggestWin = biggestWin.max(profit);
            }
        }
        return GameStats.builder()
                .played(rounds.size())
                .wins(wins)
                .wagered(wagered.setScale(2))
                .won(won.setScale(2))
                .net(won.subtract(wagered).setScale(2))
                .biggestWin(biggestWin.setScale(2))
                .build();
    }
}
