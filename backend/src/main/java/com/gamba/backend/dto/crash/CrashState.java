package com.gamba.backend.dto.crash;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// The round as every player sees it. Times are server epoch milliseconds; the page works out the
// multiplier itself from startedAt, using serverNow to line its clock up with the server's.
@Data
@Builder
public class CrashState {
    private UUID roundId;
    // BETTING, RUNNING or CRASHED
    private String phase;
    private long serverNow;
    private long bettingEndsAt;
    // Set once the round is flying
    private Long startedAt;
    // Only told once the round has crashed
    private BigDecimal crashPoint;
    private List<PlayerBet> bets;
    // The latest crash points, newest first
    private List<BigDecimal> history;

    // One player's bet; cashedOutAt and payout stay null until they cash out
    public record PlayerBet(String username, BigDecimal amount, BigDecimal cashedOutAt, BigDecimal payout) {
    }
}
