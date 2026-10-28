package com.gamba.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

// The maths of Crash, free of the database so it can be tested on its own.
// The multiplier grows as e^(0.00006·ms) of flight: 2× after about 12 s, 10× after about 38 s.
// frontend/src/crash.js draws the same curve.
public final class CrashRules {

    public static final long BETTING_MS = 8_000;
    public static final long RESULT_MS = 4_000;
    public static final double GROWTH_PER_MS = 0.00006;
    public static final BigDecimal MIN_BET = new BigDecimal("0.10");
    public static final BigDecimal MIN_AUTO_CASHOUT = new BigDecimal("1.01");
    public static final BigDecimal MAX_MULTIPLIER = new BigDecimal("1000.00");

    private CrashRules() {
    }

    // The multiplier after `ms` of flight, cut to hundredths
    public static BigDecimal multiplierAt(long ms) {
        double multiplier = Math.exp(GROWTH_PER_MS * Math.max(0, ms));
        // The nudge keeps a floating-point 1.9999999 from showing as 1.99
        long hundredths = (long) Math.floor(multiplier * 100 + 1e-6);
        return BigDecimal.valueOf(hundredths, 2).min(MAX_MULTIPLIER);
    }

    // How long the flight takes to reach `multiplier`
    public static long msUntil(BigDecimal multiplier) {
        return (long) Math.ceil(Math.log(multiplier.doubleValue()) / GROWTH_PER_MS);
    }

    // Where a round crashes, from r drawn uniformly from [0, 1). The chance to reach x is
    // 0.99 / x, so the house keeps 1 %; about 1 round in 50 crashes straight away at 1.00×.
    public static BigDecimal crashPoint(double r) {
        long hundredths = (long) Math.floor(99 / (1 - r));
        return BigDecimal.valueOf(hundredths, 2).max(BigDecimal.ONE.setScale(2)).min(MAX_MULTIPLIER);
    }

    public static BigDecimal payout(BigDecimal bet, BigDecimal multiplier) {
        return bet.multiply(multiplier).setScale(2, RoundingMode.FLOOR);
    }

    public static void checkBet(BigDecimal amount, BigDecimal autoCashout) {
        if (amount == null || amount.compareTo(MIN_BET) < 0 || amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Bets start at 0.10 with at most 2 decimals");
        }
        if (autoCashout != null && (autoCashout.compareTo(MIN_AUTO_CASHOUT) < 0
                || autoCashout.compareTo(MAX_MULTIPLIER) > 0
                || autoCashout.stripTrailingZeros().scale() > 2)) {
            throw new IllegalArgumentException("Auto cash-out must be between 1.01× and 1000×");
        }
    }
}
