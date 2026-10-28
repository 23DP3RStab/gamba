package com.gamba.backend.dto;

import com.gamba.backend.model.Wallet;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

// The player's balance as it is in the database right now
@Data
@Builder
public class BalanceResponse {
    private BigDecimal balance;
    private String currency;

    public static BalanceResponse from(Wallet wallet) {
        return BalanceResponse.builder()
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .build();
    }
}
