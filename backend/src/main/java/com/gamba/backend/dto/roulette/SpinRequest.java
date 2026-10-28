package com.gamba.backend.dto.roulette;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class SpinRequest {

    // { betKey: amount }, keys as in RouletteBets
    @NotEmpty(message = "Place a bet first")
    private Map<String, BigDecimal> bets;
}
