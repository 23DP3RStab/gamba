package com.gamba.backend.model;

public enum CrashRoundStatus {
    BETTING,
    RUNNING,
    CRASHED,
    // Cut short by a restart; its bets were refunded
    CANCELLED
}
