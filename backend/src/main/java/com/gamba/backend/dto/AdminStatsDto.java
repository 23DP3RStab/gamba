package com.gamba.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AdminStatsDto {
    private long totalUsers;
    private long activeUsers;
    private long suspendedUsers;
    private BigDecimal totalPlayerFunds;
}