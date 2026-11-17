package com.gamba.backend.model;

public enum CrashBetStatus {
    IN_PROGRESS,
    CASHED_OUT,
    LOST,
    // The round was cut short by a restart and the bet paid back
    REFUNDED
}
