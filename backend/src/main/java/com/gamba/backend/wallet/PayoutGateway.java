package com.gamba.backend.wallet;

public interface PayoutGateway {
    record Result(String batchId, String itemId, String transactionId, String status) {}
    String submit(WithdrawalLedger.Withdrawal withdrawal);
    Result check(WithdrawalLedger.Withdrawal withdrawal);
}
