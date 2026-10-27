package com.gamba.backend.dto.piggies;

import com.gamba.backend.model.PiggiesGameStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class PiggiesGameResponse {
    private UUID gameId;
    private BigDecimal betAmount;
    private int housesDown;
    // Whether the house the wolf just blew at fell; null when nothing was blown
    private Boolean fell;
    private PiggiesGameStatus status;
    private BigDecimal payout;
    private BigDecimal balance;
}
