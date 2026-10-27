package com.gamba.backend.dto.mines;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class RevealTileRequest {

    @Min(value = 0, message = "Tile index must be between 0 and 24")
    @Max(value = 24, message = "Tile index must be between 0 and 24")
    private int tileIndex;
}