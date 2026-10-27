package com.gamba.backend.wallet;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PayoutRequestTests {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://api-m.sandbox.paypal.com");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final PayPalPayoutClient client = new PayPalPayoutClient(builder.build(), "client", "secret");
    private final WithdrawalLedger.Withdrawal withdrawal = new WithdrawalLedger.Withdrawal(UUID.randomUUID(), UUID.randomUUID(),
            new BigDecimal("5.00"), "buyer@personal.example.com", "EUR", "PROCESSING", "PENDING", "BATCH1", null,
            null, null, Instant.now(), 1);

    private void token() {
        server.expect(requestTo("https://api-m.sandbox.paypal.com/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Basic Y2xpZW50OnNlY3JldA=="))
                .andRespond(withSuccess("{\"access_token\":\"token\"}", MediaType.APPLICATION_JSON));
    }
    private String batchHeader() {
        return "\"batch_header\":{\"payout_batch_id\":\"BATCH1\",\"batch_status\":\"PENDING\","
                + "\"sender_batch_header\":{\"sender_batch_id\":\"" + withdrawal.id() + "\"}}";
    }
    @Test void submitUsesSandboxRecipientAndStableBatchAndItemIds() {
        token();
        server.expect(requestTo("https://api-m.sandbox.paypal.com/v1/payments/payouts"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("PayPal-Request-Id", withdrawal.id().toString()))
                .andExpect(jsonPath("$.sender_batch_header.sender_batch_id").value(withdrawal.id().toString()))
                .andExpect(jsonPath("$.items[0].sender_item_id").value(withdrawal.id().toString()))
                .andExpect(jsonPath("$.items[0].recipient_type").value("EMAIL"))
                .andExpect(jsonPath("$.items[0].recipient_wallet").value("PAYPAL"))
                .andExpect(jsonPath("$.items[0].receiver").value(withdrawal.recipient()))
                .andExpect(jsonPath("$.items[0].amount.value").value("5.00"))
                .andExpect(jsonPath("$.items[0].amount.currency").value("EUR"))
                .andRespond(withSuccess("{" + batchHeader() + "}", MediaType.APPLICATION_JSON));
        assertEquals("BATCH1", client.submit(withdrawal)); server.verify();
    }
    @Test void duplicateBatchErrorRecoversOriginalBatchWithoutInventingANewId() {
        token();
        server.expect(requestTo("https://api-m.sandbox.paypal.com/v1/payments/payouts"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("""
                {"name":"DUPLICATE_BATCH_ID","links":[{"href":"https://api.sandbox.paypal.com/v1/payments/payouts/BATCH1","rel":"self"}]}
                """));
        assertEquals("BATCH1", client.submit(withdrawal)); server.verify();
    }
    @Test void duplicateErrorCannotRedirectRequestsToAnUntrustedHost() {
        token();
        server.expect(requestTo("https://api-m.sandbox.paypal.com/v1/payments/payouts"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("""
                {"name":"DUPLICATE_BATCH_ID","links":[{"href":"https://attacker.example/v1/payments/payouts/BATCH1"}]}
                """));
        PaymentException error = assertThrows(PaymentException.class, () -> client.submit(withdrawal));
        assertEquals("PAYOUT_REVIEW_REQUIRED", error.getCode()); server.verify();
    }
    @Test void insufficientBusinessFundsAreADefiniteRejection() {
        token();
        server.expect(requestTo("https://api-m.sandbox.paypal.com/v1/payments/payouts"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"name\":\"INSUFFICIENT_FUNDS\"}"));
        assertEquals("PAYOUT_INSUFFICIENT_FUNDS", assertThrows(PayoutRejectedException.class, () -> client.submit(withdrawal)).getCode());
        server.verify();
    }
    @Test void missingPayoutPermissionIsReportedExplicitly() {
        token();
        server.expect(requestTo("https://api-m.sandbox.paypal.com/v1/payments/payouts"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"name\":\"AUTHORIZATION_ERROR\"}"));
        assertEquals("PAYOUT_ACCESS_DENIED", assertThrows(PayoutRejectedException.class, () -> client.submit(withdrawal)).getCode());
        server.verify();
    }
    @Test void serverErrorIsUncertainRatherThanAConfirmedFailure() {
        token();
        server.expect(requestTo("https://api-m.sandbox.paypal.com/v1/payments/payouts"))
                .andRespond(withServerError());
        PaymentException error = assertThrows(PaymentException.class, () -> client.submit(withdrawal));
        assertFalse(error instanceof PayoutRejectedException); assertEquals("PAYOUT_UNCERTAIN", error.getCode()); server.verify();
    }
    @Test void statusUsesTheSavedBatchAndVerifiesTheIndividualItem() {
        token();
        server.expect(requestTo("https://api-m.sandbox.paypal.com/v1/payments/payouts/BATCH1"))
                .andExpect(method(HttpMethod.GET)).andRespond(withSuccess("{" + batchHeader() + ",\"items\":[{"
                        + "\"payout_item_id\":\"ITEM1\",\"transaction_id\":\"TX1\",\"transaction_status\":\"SUCCESS\","
                        + "\"payout_item\":{\"recipient_type\":\"EMAIL\",\"receiver\":\"buyer@personal.example.com\","
                        + "\"sender_item_id\":\"" + withdrawal.id() + "\",\"amount\":{\"currency\":\"EUR\",\"value\":\"5.00\"}}}]}",
                        MediaType.APPLICATION_JSON));
        assertEquals("SUCCESS", client.check(withdrawal).status()); server.verify();
    }

    private Map<String, Object> response(String batchStatus, String itemStatus, String email, String value, String currency) {
        Map<String, Object> response = new HashMap<>();
        response.put("batch_header", Map.of("payout_batch_id", "BATCH1", "batch_status", batchStatus,
                "sender_batch_header", Map.of("sender_batch_id", withdrawal.id().toString())));
        response.put("items", List.of(Map.of("payout_item_id", "ITEM1", "transaction_id", "TX1", "transaction_status", itemStatus,
                "payout_item", Map.of("recipient_type", "EMAIL", "receiver", email, "sender_item_id", withdrawal.id().toString(),
                        "amount", Map.of("value", value, "currency", currency)))));
        return response;
    }
    @Test void batchSuccessDoesNotTurnAnUnclaimedItemIntoASuccess() {
        assertEquals("UNCLAIMED", PayPalPayoutClient.verify(response("SUCCESS", "UNCLAIMED", withdrawal.recipient(), "5.00", "EUR"), withdrawal).status());
    }
    @Test void emptySuccessfulBatchIsStillPending() {
        Map<String, Object> response = response("SUCCESS", "SUCCESS", withdrawal.recipient(), "5.00", "EUR");
        response.put("items", List.of()); assertEquals("PENDING", PayPalPayoutClient.verify(response, withdrawal).status());
    }
    @Test void deniedBatchWithoutItemsCanBeRecognizedAsRejected() {
        Map<String, Object> response = response("DENIED", "FAILED", withdrawal.recipient(), "5.00", "EUR");
        response.put("items", List.of()); assertEquals("DENIED", PayPalPayoutClient.verify(response, withdrawal).status());
    }
    @Test void wrongRecipientAmountAndCurrencyAreRejected() {
        assertThrows(PaymentException.class, () -> PayPalPayoutClient.verify(response("SUCCESS", "SUCCESS", "wrong@example.com", "5.00", "EUR"), withdrawal));
        assertThrows(PaymentException.class, () -> PayPalPayoutClient.verify(response("SUCCESS", "SUCCESS", withdrawal.recipient(), "50.00", "EUR"), withdrawal));
        assertThrows(PaymentException.class, () -> PayPalPayoutClient.verify(response("SUCCESS", "SUCCESS", withdrawal.recipient(), "5.00", "USD"), withdrawal));
    }
    @Test void wrongSenderBatchCannotReleaseOrFinalizeAReservation() {
        Map<String, Object> response = response("DENIED", "FAILED", withdrawal.recipient(), "5.00", "EUR");
        response.put("batch_header", Map.of("payout_batch_id", "BATCH1", "batch_status", "DENIED",
                "sender_batch_header", Map.of("sender_batch_id", UUID.randomUUID().toString())));
        assertThrows(PaymentException.class, () -> PayPalPayoutClient.verify(response, withdrawal));
    }
}
