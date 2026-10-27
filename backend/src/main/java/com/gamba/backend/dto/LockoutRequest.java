package com.gamba.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LockoutRequest {

    // 1, 7, 30 or 180
    @NotNull(message = "Choose how long to lock the account")
    private Integer days;
}
