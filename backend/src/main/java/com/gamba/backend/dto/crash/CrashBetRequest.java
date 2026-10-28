package com.gamba.backend.dto.crash;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CrashBetRequest {

    @NotNull(message = "Bet amount is required")
    @DecimalMin(value = "0.10", message = "Minimum bet is 0.10")
    @Digits(integer = 13, fraction = 2, message = "Bet can have at most 2 decimals")
    private BigDecimal amount;

    // Optional: cash out by itself once the multiplier reaches this
    @DecimalMin(value = "1.01", message = "Auto cash-out starts at 1.01×")
    @DecimalMax(value = "1000.00", message = "Auto cash-out goes up to 1000×")
    @Digits(integer = 4, fraction = 2, message = "Auto cash-out can have at most 2 decimals")
    private BigDecimal autoCashout;
}
