package com.gamba.backend.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CrashRulesTests {

    private static BigDecimal x(String multiplier) {
        return new BigDecimal(multiplier);
    }

    @Test
    void theFlightStartsAtOneAndDoublesInAboutTwelveSeconds() {
        assertEquals(x("1.00"), CrashRules.multiplierAt(0));
        assertEquals(x("1.00"), CrashRules.multiplierAt(-50));
        assertEquals(x("1.99"), CrashRules.multiplierAt(11_550));
        assertEquals(x("2.00"), CrashRules.multiplierAt(11_553));
        assertEquals(x("1000.00"), CrashRules.multiplierAt(10_000_000));
    }

    @Test
    void reachingAMultiplierTakesExactlyTheTimeItsTargetNeeds() {
        for (String target : new String[]{"1.01", "1.50", "2.00", "3.33", "10.00", "123.45", "1000.00"}) {
            long ms = CrashRules.msUntil(x(target));
            assertEquals(x(target), CrashRules.multiplierAt(ms), target);
            assertTrue(CrashRules.multiplierAt(ms - 1).compareTo(x(target)) < 0, target);
        }
        assertEquals(0, CrashRules.msUntil(x("1.00")));
    }

    @Test
    void theCrashPointLeavesTheHouseOnePercent() {
        assertEquals(x("1.00"), CrashRules.crashPoint(0));
        assertEquals(x("1.00"), CrashRules.crashPoint(0.01));
        assertEquals(x("1.98"), CrashRules.crashPoint(0.5));
        // Reaching 2× takes r ≥ 0.505: a 49.5 % chance, 0.99 / 2
        assertEquals(x("1.99"), CrashRules.crashPoint(0.5049));
        assertEquals(x("2.00"), CrashRules.crashPoint(0.505));
        assertEquals(x("3.96"), CrashRules.crashPoint(0.75));
        assertEquals(x("1000.00"), CrashRules.crashPoint(0.9999999));
    }

    @Test
    void payoutsAreRoundedDownToCents() {
        assertEquals(x("2.46"), CrashRules.payout(x("1.23"), x("2.00")));
        assertEquals(x("0.15"), CrashRules.payout(x("0.10"), x("1.59")));
        assertEquals(x("33.29"), CrashRules.payout(x("9.99"), x("3.333")));
    }

    @Test
    void betsAndAutoCashOutsAreChecked() {
        assertDoesNotThrow(() -> CrashRules.checkBet(x("0.10"), null));
        assertDoesNotThrow(() -> CrashRules.checkBet(x("5"), x("1.01")));
        assertDoesNotThrow(() -> CrashRules.checkBet(x("5"), x("1000")));
        assertThrows(IllegalArgumentException.class, () -> CrashRules.checkBet(x("0.09"), null));
        assertThrows(IllegalArgumentException.class, () -> CrashRules.checkBet(x("1.005"), null));
        assertThrows(IllegalArgumentException.class, () -> CrashRules.checkBet(null, null));
        assertThrows(IllegalArgumentException.class, () -> CrashRules.checkBet(x("5"), x("1.00")));
        assertThrows(IllegalArgumentException.class, () -> CrashRules.checkBet(x("5"), x("1000.01")));
        assertThrows(IllegalArgumentException.class, () -> CrashRules.checkBet(x("5"), x("2.005")));
    }
}
