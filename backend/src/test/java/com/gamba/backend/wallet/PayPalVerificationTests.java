package com.gamba.backend.wallet;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class PayPalVerificationTests {
    private final WalletLedger.Deposit deposit = new WalletLedger.Deposit(UUID.randomUUID(), UUID.randomUUID(),
            new BigDecimal("5.00"), "EUR", "ORDER1", UUID.randomUUID().toString(), null, null);

    private Map<String, Object> response(String currency, String amount, String captureStatus) {
        return Map.of("id", "ORDER1", "status", "COMPLETED", "purchase_units", List.of(Map.of(
                "custom_id", deposit.id().toString(), "payments", Map.of("captures", List.of(Map.of(
                        "id", "CAPTURE1", "status", captureStatus,
                        "amount", Map.of("currency_code", currency, "value", amount)))))));
    }

    @Test void completedMatchingCaptureIsAccepted() {
        PayPalGateway.Capture result = PayPalClient.verifyCapture(response("EUR", "5.00", "COMPLETED"), deposit);
        assertEquals(new BigDecimal("5.00"), result.amount());
        assertEquals("CAPTURE1", result.captureId());
    }
    @Test void wrongAmountIsRejected() {
        assertThrows(PaymentException.class, () -> PayPalClient.verifyCapture(response("EUR", "500.00", "COMPLETED"), deposit));
    }
    @Test void wrongCurrencyIsRejected() {
        assertThrows(PaymentException.class, () -> PayPalClient.verifyCapture(response("USD", "5.00", "COMPLETED"), deposit));
    }
    @Test void pendingCaptureCannotCredit() {
        PaymentException error = assertThrows(PaymentException.class,
                () -> PayPalClient.verifyCapture(response("EUR", "5.00", "PENDING"), deposit));
        assertEquals("PAYMENT_PENDING", error.getCode());
    }
    @Test void unapprovedOrderIsReportedSeparately() {
        PaymentException error = assertThrows(PaymentException.class,
                () -> PayPalClient.verifyCapture(Map.of("id", "ORDER1", "status", "CREATED"), deposit));
        assertEquals("ORDER_NOT_APPROVED", error.getCode());
    }
    @Test void differentOrderIsRejected() {
        Map<String, Object> result = new HashMap<>(response("EUR", "5.00", "COMPLETED"));
        result.put("id", "OTHERORDER");
        assertThrows(PaymentException.class, () -> PayPalClient.verifyCapture(result, deposit));
    }
    @Test void missingCaptureAndMalformedAmountsAreRejected() {
        assertThrows(PaymentException.class, () -> PayPalClient.verifyCapture(
                Map.of("id", "ORDER1", "status", "COMPLETED"), deposit));
        assertThrows(PaymentException.class, () -> PayPalClient.verifyCapture(response("EUR", "abc", "COMPLETED"), deposit));
    }
}
