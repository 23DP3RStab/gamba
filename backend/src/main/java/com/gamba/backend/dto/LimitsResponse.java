package com.gamba.backend.dto;

import com.gamba.backend.model.User;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class LimitsResponse {
    private BigDecimal dailyLossLimit;
    private BigDecimal lostToday;
    // How much more can be lost today; null without a limit
    private BigDecimal leftToday;
    // A raised or removed limit waiting for its cooling-off period; pendingFrom null when none
    private BigDecimal pendingLossLimit;
    private Instant pendingFrom;
    private Instant lockedUntil;

    public static LimitsResponse of(User user, BigDecimal lostToday) {
        BigDecimal limit = user.getDailyLossLimit();
        return LimitsResponse.builder()
                .dailyLossLimit(limit)
                .lostToday(lostToday)
                .leftToday(limit == null ? null : limit.subtract(lostToday).max(BigDecimal.ZERO))
                .pendingLossLimit(user.getPendingLossLimit())
                .pendingFrom(user.getPendingLimitFrom())
                .lockedUntil(user.getLockedUntil())
                .build();
    }
}
