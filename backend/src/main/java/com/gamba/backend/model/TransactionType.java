package com.gamba.backend.model;

public enum TransactionType {
    BET,
    WIN,
    DEPOSIT,
    WITHDRAWAL,
    ADMIN_ADJUSTMENT,
    // A Crash bet paid back because a restart cut its round short
    REFUND
}