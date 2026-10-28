package com.gamba.backend.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RouletteBetsTests {

    private static BigDecimal pays(String key, String amount, int number) {
        return RouletteBets.returnFor(key, new BigDecimal(amount), number);
    }

    @Test
    void hasTheSameBetsAsTheTable() {
        // 37 straight, 108 inside bets on the lines, 3 columns, 3 dozens, 6 even-money
        assertEquals(157, RouletteBets.BETS.size());
        assertEquals(Set.of(2, 3, 5, 6), RouletteBets.BETS.get("corner-2"));
        assertEquals(Set.of(13, 14, 15, 16, 17, 18), RouletteBets.BETS.get("line-13"));
        assertEquals(Set.of(0, 1, 2, 3), RouletteBets.BETS.get("first-four"));
        assertEquals(12, RouletteBets.BETS.get("column-1").size());
        assertTrue(RouletteBets.BETS.get("column-1").containsAll(Set.of(1, 4, 34)));
        assertEquals(18, RouletteBets.BETS.get("red").size());
        assertEquals(18, RouletteBets.BETS.get("black").size());
    }

    @Test
    void everyBetCoversValidNumbersAndPaysWholeMultiples() {
        RouletteBets.BETS.forEach((key, numbers) -> {
            assertTrue(numbers.stream().allMatch(n -> n >= 0 && n <= 36), key);
            assertEquals(0, 36 % numbers.size(), key);
        });
    }

    @Test
    void winningBetsReturnTheirStakeTimes36OverTheNumbersCovered() {
        assertEquals(new BigDecimal("360"), pays("n17", "10", 17));
        assertEquals(new BigDecimal("180"), pays("split-0-3", "10", 0));
        assertEquals(new BigDecimal("90"), pays("corner-1", "10", 5));
        assertEquals(new BigDecimal("30"), pays("dozen-2", "10", 13));
        assertEquals(new BigDecimal("20"), pays("red", "10", 1));
        assertEquals(new BigDecimal("2.50"), pays("even", "1.25", 36));
    }

    @Test
    void losingBetsReturnNothing() {
        assertEquals(BigDecimal.ZERO, pays("red", "10", 0));
        assertEquals(BigDecimal.ZERO, pays("n17", "10", 18));
        assertEquals(BigDecimal.ZERO, pays("high", "10", 18));
    }

    @Test
    void unknownBetsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> pays("split-5-30", "10", 5));
    }
}
