package com.gamba.backend.dto.crash;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CrashBetResponse {
    private BigDecimal amount;
    private BigDecimal autoCashout;
    private BigDecimal balance;
}
