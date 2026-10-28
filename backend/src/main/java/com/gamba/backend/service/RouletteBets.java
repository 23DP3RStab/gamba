package com.gamba.backend.service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// European roulette bets, with the same keys the frontend table sends (frontend/src/roulette.js).
// Each bet covers a set of numbers; a winning bet returns amount × 36 / numbers, stake included:
// straight 36×, split 18×, street 12×, corner 9×, six line 6×, dozen/column 3×, even-money 2×.
public final class RouletteBets {

    private static final Set<Integer> RED =
            Set.of(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);

    public static final Map<String, Set<Integer>> BETS = build();

    private RouletteBets() {
    }

    // What `amount` on `key` returns when `number` wins, stake included; zero for a losing bet
    public static BigDecimal returnFor(String key, BigDecimal amount, int number) {
        Set<Integer> numbers = BETS.get(key);
        if (numbers == null) {
            throw new IllegalArgumentException("Unknown bet: " + key);
        }
        if (!numbers.contains(number)) return BigDecimal.ZERO;
        return amount.multiply(BigDecimal.valueOf(36 / numbers.size()));
    }

    // The table's number grid has 12 columns (1-3, 4-6, ... 34-36) and 3 rows,
    // with the top row holding 3, 6, ... 36 and the bottom row 1, 4, ... 34
    private static int numAt(int col, int row) {
        return 3 * col + 3 - row;
    }

    private static Map<String, Set<Integer>> build() {
        Map<String, Set<Integer>> bets = new LinkedHashMap<>();
        for (int n = 0; n <= 36; n++) {
            bets.put("n" + n, Set.of(n));
        }

        for (int row = 0; row < 3; row++) {
            int n = numAt(0, row);
            bets.put("split-0-" + n, Set.of(0, n));
        }
        bets.put("trio-0-2-3", Set.of(0, 2, 3));
        bets.put("trio-0-1-2", Set.of(0, 1, 2));
        bets.put("first-four", Set.of(0, 1, 2, 3));

        for (int col = 0; col < 12; col++) {
            for (int row = 0; row < 3; row++) {
                int n = numAt(col, row);
                if (col < 11) bets.put("split-" + n + "-" + (n + 3), Set.of(n, n + 3));
                if (row < 2) bets.put("split-" + (n - 1) + "-" + n, Set.of(n - 1, n));
                if (col < 11 && row < 2) bets.put("corner-" + (n - 1), Set.of(n - 1, n, n + 2, n + 3));
            }
            int first = 3 * col + 1;
            bets.put("street-" + first, Set.of(first, first + 1, first + 2));
            if (col < 11) bets.put("line-" + first, numbers(n -> n >= first && n <= first + 5));
        }

        for (int c = 1; c <= 3; c++) {
            int column = c;
            bets.put("column-" + c, numbers(n -> (n - column) % 3 == 0));
        }
        for (int d = 1; d <= 3; d++) {
            int dozen = d;
            bets.put("dozen-" + d, numbers(n -> n > 12 * (dozen - 1) && n <= 12 * dozen));
        }
        bets.put("low", numbers(n -> n <= 18));
        bets.put("even", numbers(n -> n % 2 == 0));
        bets.put("red", numbers(RED::contains));
        bets.put("black", numbers(n -> !RED.contains(n)));
        bets.put("odd", numbers(n -> n % 2 == 1));
        bets.put("high", numbers(n -> n >= 19));
        return Collections.unmodifiableMap(bets);
    }

    // The numbers 1-36 matching `test`
    private static Set<Integer> numbers(IntPredicate test) {
        return IntStream.rangeClosed(1, 36).filter(test).boxed().collect(Collectors.toUnmodifiableSet());
    }
}
