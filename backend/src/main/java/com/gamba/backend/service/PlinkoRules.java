package com.gamba.backend.service;

import com.gamba.backend.model.PlinkoRisk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// The maths of Plinko, free of the database so it can be tested on its own. The ball bounces off
// 12 rows of pegs, each time left or right with equal chance, and lands in one of 13 slots: the
// number of times it went right. The edge slots are rare (1 in 4096) and pay the most.
public final class PlinkoRules {

    public static final int ROWS = 12;

    // Payout per slot, left to right; every table pays back about 99 % on average
    public static final Map<PlinkoRisk, List<BigDecimal>> MULTIPLIERS = tables();

    private PlinkoRules() {
    }

    private static Map<PlinkoRisk, List<BigDecimal>> tables() {
        Map<PlinkoRisk, List<BigDecimal>> tables = new EnumMap<>(PlinkoRisk.class);
        tables.put(PlinkoRisk.LOW, mirrored("10", "3", "1.6", "1.4", "1.1", "1.0", "0.5"));
        tables.put(PlinkoRisk.MEDIUM, mirrored("33", "11", "4", "2", "1.1", "0.6", "0.3"));
        tables.put(PlinkoRisk.HIGH, mirrored("170", "24", "8.1", "2", "0.7", "0.2", "0.2"));
        return Collections.unmodifiableMap(tables);
    }

    // The edge-to-middle half, mirrored into all 13 slots
    private static List<BigDecimal> mirrored(String... half) {
        List<BigDecimal> slots = new ArrayList<>();
        for (String multiplier : half) {
            slots.add(new BigDecimal(multiplier).setScale(2));
        }
        for (int i = half.length - 2; i >= 0; i--) {
            slots.add(slots.get(i));
        }
        return List.copyOf(slots);
    }

    // The slot a path of L and R bounces ends in: how many times the ball went right
    public static int slotOf(String path) {
        if (path == null || path.length() != ROWS || !path.matches("[LR]+")) {
            throw new IllegalArgumentException("A path is 12 bounces of L or R");
        }
        return (int) path.chars().filter(c -> c == 'R').count();
    }

    public static BigDecimal payout(BigDecimal bet, BigDecimal multiplier) {
        return bet.multiply(multiplier).setScale(2, RoundingMode.FLOOR);
    }

    // The share of each bet paid back on average: every slot's multiplier times its chance,
    // C(12, slot) / 4096
    static double returnToPlayer(PlinkoRisk risk) {
        List<BigDecimal> slots = MULTIPLIERS.get(risk);
        double total = 0;
        long ways = 1; // C(12, slot)
        for (int slot = 0; slot <= ROWS; slot++) {
            total += slots.get(slot).doubleValue() * ways;
            ways = ways * (ROWS - slot) / (slot + 1);
        }
        return total / Math.pow(2, ROWS);
    }
}
