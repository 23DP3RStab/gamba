package com.gamba.backend.dto.plinko;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlinkoDropResponse {
    // The 12 bounces as L and R, top to bottom, for the board to animate
    private String path;
    private int slot;
    private BigDecimal multiplier;
    private BigDecimal payout;
    private BigDecimal balance;
}
