package com.gamba.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdjustBalanceRequest {

    @NotNull(message = "Adjustment amount is required")
    private BigDecimal amount;

    private String reason;
}