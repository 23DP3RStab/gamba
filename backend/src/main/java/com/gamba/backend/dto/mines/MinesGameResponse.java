package com.gamba.backend.dto.mines;

import com.gamba.backend.model.MinesGameStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MinesGameResponse {
    private UUID gameId;
    private BigDecimal betAmount;
    private int mineCount;
    private List<Integer> revealedTiles;
    private BigDecimal currentMultiplier;
    private BigDecimal nextMultiplier;
    private MinesGameStatus status;
    private BigDecimal payout;
    private List<Integer> minePositions;
    private BigDecimal balance;
}