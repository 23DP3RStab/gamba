package com.gamba.backend.dto;

import com.gamba.backend.model.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TransactionDto {
    private UUID id;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private TransactionType type;
    private String description;
    private UUID referenceId;
    private Instant createdAt;
}