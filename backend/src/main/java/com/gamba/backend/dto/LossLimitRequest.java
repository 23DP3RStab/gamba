package com.gamba.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LossLimitRequest {

    // null removes the limit
    @DecimalMin(value = "1.00", message = "The limit must be at least 1.00")
    @DecimalMax(value = "100000.00", message = "The limit can be at most 100000.00")
    @Digits(integer = 6, fraction = 2, message = "The limit can have at most 2 decimals")
    private BigDecimal dailyLossLimit;
}
