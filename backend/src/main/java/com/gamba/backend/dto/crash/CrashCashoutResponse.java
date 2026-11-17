package com.gamba.backend.dto.crash;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CrashCashoutResponse {
    private BigDecimal multiplier;
    private BigDecimal payout;
    private BigDecimal balance;
}
