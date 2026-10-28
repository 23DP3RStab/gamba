package com.gamba.backend.wallet;

import java.math.BigDecimal;

public interface PayPalGateway {
    record Capture(String orderId, String captureId, BigDecimal amount, String currency) {}
    void checkConnection();
    String createOrder(WalletLedger.Deposit deposit);
    Capture capture(WalletLedger.Deposit deposit);
}
