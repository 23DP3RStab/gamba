package com.gamba.backend.wallet;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PayPalRequestTests {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://api-m.sandbox.paypal.com");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final PayPalClient client = new PayPalClient(builder.build(), "client", "secret");
    private final WalletLedger.Deposit deposit = new WalletLedger.Deposit(UUID.randomUUID(), UUID.randomUUID(),
            new BigDecimal("5.00"), "EUR", "ORDER1", UUID.randomUUID().toString(), null, null);

    private void token() {
        server.expect(requestTo("https://api-m.sandbox.paypal.com/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Basic Y2xpZW50OnNlY3JldA=="))
                .andExpect(content().string("grant_type=client_credentials"))
                .andRespond(withSuccess("{\"access_token\":\"test-token\"}", MediaType.APPLICATION_JSON));
    }

    private void getOrder(String body) {
        server.expect(requestTo("https://api-m.sandbox.paypal.com/v2/checkout/orders/ORDER1"))
                .andExpect(method(HttpMethod.GET)).andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private String completed() {
        return """
                {"id":"ORDER1","status":"COMPLETED","purchase_units":[
                  {"payments":{"captures":[{"id":"CAP1","status":"COMPLETED",
                    "amount":{"currency_code":"EUR","value":"5.00"}}]}}]}
                """;
    }

    @Test void creationUsesServerAmountSandboxAndPersistentRequestId() {
        token();
        server.expect(requestTo("https://api-m.sandbox.paypal.com/v2/checkout/orders"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("PayPal-Request-Id", deposit.id().toString()))
                .andExpect(jsonPath("$.purchase_units[0].amount.value").value("5.00"))
                .andExpect(jsonPath("$.purchase_units[0].amount.currency_code").value("EUR"))
                .andExpect(jsonPath("$.purchase_units[0].custom_id").value(deposit.id().toString()))
                .andRespond(withSuccess("{\"id\":\"ORDER1\",\"status\":\"CREATED\"}", MediaType.APPLICATION_JSON));
        assertEquals("ORDER1", client.createOrder(deposit));
        server.verify();
    }

    @Test void captureUsesSavedIdempotencyKeyAfterCheckingApproval() {
        token(); getOrder("{\"id\":\"ORDER1\",\"status\":\"APPROVED\"}");
        server.expect(requestTo("https://api-m.sandbox.paypal.com/v2/checkout/orders/ORDER1/capture"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("PayPal-Request-Id", deposit.captureRequestId()))
                .andRespond(withSuccess(completed(), MediaType.APPLICATION_JSON));
        assertEquals("CAP1", client.capture(deposit).captureId());
        server.verify();
    }

    @Test void alreadyCompletedOrderIsReconciledWithoutSendingAnotherCapture() {
        token(); getOrder(completed());
        assertEquals("CAP1", client.capture(deposit).captureId());
        server.verify();
    }

    @Test void simultaneousCaptureConflictIsReconciledFromProvider() {
        token(); getOrder("{\"id\":\"ORDER1\",\"status\":\"APPROVED\"}");
        server.expect(requestTo("https://api-m.sandbox.paypal.com/v2/checkout/orders/ORDER1/capture"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));
        getOrder(completed());
        assertEquals("CAP1", client.capture(deposit).captureId());
        server.verify();
    }

    @Test void providerFailureDoesNotReturnASuccessfulCapture() {
        token();
        server.expect(requestTo("https://api-m.sandbox.paypal.com/v2/checkout/orders/ORDER1"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        PaymentException error = assertThrows(PaymentException.class, () -> client.capture(deposit));
        assertEquals("PAYPAL_UNAVAILABLE", error.getCode());
        server.verify();
    }
}
