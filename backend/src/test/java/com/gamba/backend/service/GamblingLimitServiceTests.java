package com.gamba.backend.service;

import com.gamba.backend.model.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GamblingLimitServiceTests {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    private static BigDecimal eur(String amount) {
        return new BigDecimal(amount);
    }

    private static User withLimit(String limit) {
        User user = new User();
        user.setDailyLossLimit(limit == null ? null : eur(limit));
        return user;
    }

    @Test
    void aFirstOrLowerLimitAppliesImmediately() {
        User user = withLimit(null);
        GamblingLimitService.changeLimit(user, eur("50"), NOW);
        assertEquals(eur("50"), user.getDailyLossLimit());

        GamblingLimitService.changeLimit(user, eur("20"), NOW);
        assertEquals(eur("20"), user.getDailyLossLimit());
        assertNull(user.getPendingLimitFrom());
    }

    @Test
    void aHigherLimitWaits24Hours() {
        User user = withLimit("20");
        GamblingLimitService.changeLimit(user, eur("100"), NOW);
        assertEquals(eur("20"), user.getDailyLossLimit());
        assertEquals(eur("100"), user.getPendingLossLimit());
        assertEquals(NOW.plus(Duration.ofHours(24)), user.getPendingLimitFrom());

        assertFalse(GamblingLimitService.applyPending(user, NOW.plus(Duration.ofHours(23))));
        assertEquals(eur("20"), user.getDailyLossLimit());

        assertTrue(GamblingLimitService.applyPending(user, NOW.plus(Duration.ofHours(24))));
        assertEquals(eur("100"), user.getDailyLossLimit());
        assertNull(user.getPendingLimitFrom());
    }

    @Test
    void removingTheLimitAlsoWaits24Hours() {
        User user = withLimit("20");
        GamblingLimitService.changeLimit(user, null, NOW);
        assertEquals(eur("20"), user.getDailyLossLimit());
        assertNotNull(user.getPendingLimitFrom());

        GamblingLimitService.applyPending(user, NOW.plus(Duration.ofDays(1)));
        assertNull(user.getDailyLossLimit());
    }

    @Test
    void loweringTheLimitCancelsAPendingRaise() {
        User user = withLimit("20");
        GamblingLimitService.changeLimit(user, eur("100"), NOW);
        GamblingLimitService.changeLimit(user, eur("10"), NOW.plus(Duration.ofHours(1)));
        assertEquals(eur("10"), user.getDailyLossLimit());
        assertNull(user.getPendingLossLimit());
        assertNull(user.getPendingLimitFrom());
    }

    @Test
    void aLockoutCanOnlyBeExtended() {
        User user = new User();
        GamblingLimitService.lockOut(user, 30, NOW);
        GamblingLimitService.lockOut(user, 1, NOW);
        assertEquals(NOW.plus(Duration.ofDays(30)), user.getLockedUntil());

        GamblingLimitService.lockOut(user, 180, NOW);
        assertEquals(NOW.plus(Duration.ofDays(180)), user.getLockedUntil());

        assertThrows(IllegalArgumentException.class, () -> GamblingLimitService.lockOut(user, 2, NOW));
    }

    @Test
    void noBetsWhileLockedOut() {
        User user = new User();
        user.setLockedUntil(NOW.plus(Duration.ofHours(1)));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> GamblingLimitService.checkBet(user, eur("1"), BigDecimal.ZERO, NOW));
        assertTrue(error.getMessage().startsWith("Your account is locked until"));

        assertDoesNotThrow(() -> GamblingLimitService.checkBet(user, eur("1"), BigDecimal.ZERO,
                NOW.plus(Duration.ofHours(1))));
    }

    @Test
    void aBetMayNotTakeTodaysLossesOverTheLimit() {
        User user = withLimit("50");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> GamblingLimitService.checkBet(user, eur("10"), eur("45"), NOW));
        assertEquals("This bet would go over your daily loss limit. You can lose €5.00 more today",
                error.getMessage());

        assertDoesNotThrow(() -> GamblingLimitService.checkBet(user, eur("5"), eur("45"), NOW));
        // Winnings today leave more room
        assertDoesNotThrow(() -> GamblingLimitService.checkBet(user, eur("60"), eur("-20"), NOW));
    }
}
