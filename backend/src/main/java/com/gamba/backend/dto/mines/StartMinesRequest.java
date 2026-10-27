package com.gamba.backend.dto.mines;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StartMinesRequest {

    @NotNull(message = "Bet amount is required")
    @DecimalMin(value = "0.10", message = "Minimum bet is 0.10")
    private BigDecimal betAmount;

    @Min(value = 1, message = "At least 1 mine is required")
    @Max(value = 24, message = "Maximum 24 mines allowed")
    private int mineCount;
}