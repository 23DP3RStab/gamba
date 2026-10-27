package com.gamba.backend.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameStatsTests {

    private static GameStats.Round round(String bet, String payout) {
        return new GameStats.Round(new BigDecimal(bet), new BigDecimal(payout));
    }

    @Test
    void addsUpRoundsAndCountsOnlyProfitableOnesAsWins() {
        GameStats stats = GameStats.of(List.of(
                round("10", "0"),
                round("10", "25"),
                round("5", "5"), // got the stake back: not a win
                round("20", "30")));

        assertEquals(4, stats.getPlayed());
        assertEquals(2, stats.getWins());
        assertEquals(new BigDecimal("45.00"), stats.getWagered());
        assertEquals(new BigDecimal("60.00"), stats.getWon());
        assertEquals(new BigDecimal("15.00"), stats.getNet());
        assertEquals(new BigDecimal("15.00"), stats.getBiggestWin());
    }

    @Test
    void noRoundsMeansZeros() {
        GameStats stats = GameStats.of(List.of());
        assertEquals(0, stats.getPlayed());
        assertEquals(new BigDecimal("0.00"), stats.getNet());
        assertEquals(new BigDecimal("0.00"), stats.getBiggestWin());
    }
}
