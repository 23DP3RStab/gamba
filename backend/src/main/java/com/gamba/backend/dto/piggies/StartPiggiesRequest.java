package com.gamba.backend.dto.piggies;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StartPiggiesRequest {

    @NotNull(message = "Bet amount is required")
    @DecimalMin(value = "0.10", message = "Minimum bet is 0.10")
    @Digits(integer = 13, fraction = 2, message = "Bet can have at most 2 decimals")
    private BigDecimal betAmount;
}
