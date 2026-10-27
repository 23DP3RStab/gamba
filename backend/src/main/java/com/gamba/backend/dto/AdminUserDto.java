package com.gamba.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AdminUserDto {
    private UUID id;
    private String username;
    private String email;
    private String role;
    private boolean active;
    private BigDecimal balance;
    private String currency;
    private Instant createdAt;
}