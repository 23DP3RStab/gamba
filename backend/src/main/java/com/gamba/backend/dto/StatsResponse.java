package com.gamba.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatsResponse {
    private GameStats mines;
    private GameStats roulette;
    private GameStats piggies;
    private GameStats crash;
}
