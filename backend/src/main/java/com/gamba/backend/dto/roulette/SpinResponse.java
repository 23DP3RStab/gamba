package com.gamba.backend.dto.roulette;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SpinResponse {
    private int number;
    private BigDecimal totalBet;
    private BigDecimal payout;
    private BigDecimal balance;
}
