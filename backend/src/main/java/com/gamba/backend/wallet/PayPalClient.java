package com.gamba.backend.wallet;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

@Service
public class PayPalClient implements PayPalGateway {
    private final RestClient paypal;
    private final String clientId;
    private final String clientSecret;

    @Autowired
    public PayPalClient(@Value("${paypal.client-id}") String clientId,
                        @Value("${paypal.client-secret}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(20));
        // This project intentionally cannot send live PayPal payments.
        paypal = RestClient.builder().baseUrl("https://api-m.sandbox.paypal.com")
                .requestFactory(factory).build();
    }

    PayPalClient(RestClient paypal, String clientId, String clientSecret) {
        this.paypal = paypal;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public void checkConnection() {
        try { token(); } catch (RestClientException ex) { throw providerError(ex); }
    }

    @Override
    public String createOrder(WalletLedger.Deposit deposit) {
        try {
            String accessToken = token();
            Map<String, Object> body = Map.of(
                    "intent", "CAPTURE",
                    "purchase_units", List.of(Map.of(
                            "description", "Gamba Sandbox test deposit",
                            "custom_id", deposit.id().toString(),
                            "amount", Map.of("currency_code", deposit.currency(),
                                    "value", deposit.amount().toPlainString()))),
                    "application_context", Map.of("shipping_preference", "NO_SHIPPING"));
            Map<?, ?> result = paypal.post().uri("/v2/checkout/orders")
                    .headers(h -> {
                        h.setBearerAuth(accessToken);
                        h.set("PayPal-Request-Id", deposit.id().toString());
                        h.set("Prefer", "return=representation");
                    }).contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().body(Map.class);
            if (result == null || !(result.get("id") instanceof String id)
                    || !id.matches("[A-Z0-9]{1,36}")) {
                throw new PaymentException(HttpStatus.BAD_GATEWAY, "INVALID_ORDER_RESPONSE",
                        "PayPal neatgrieza derīgu pasūtījuma ID.");
            }
            return id;
        } catch (RestClientException ex) { throw providerError(ex); }
    }

    @Override
    public Capture capture(WalletLedger.Deposit deposit) {
        try {
            String accessToken = token();
            Map<?, ?> response = getOrder(deposit.orderId(), accessToken);
            if (response == null) throw invalidResponse();
            if ("APPROVED".equals(response.get("status"))) {
                try {
                    response = paypal.post().uri("/v2/checkout/orders/{id}/capture", deposit.orderId())
                            .headers(h -> {
                                h.setBearerAuth(accessToken);
                                h.set("PayPal-Request-Id", deposit.captureRequestId());
                                h.set("Prefer", "return=representation");
                            }).contentType(MediaType.APPLICATION_JSON).body(Map.of())
                            .retrieve().body(Map.class);
                } catch (RestClientResponseException ex) {
                    // Another request may have captured the same order. Reconcile from PayPal,
                    // then let the database transaction decide whether it was already credited.
                    if (ex.getStatusCode().value() == 422 || ex.getStatusCode().value() == 409) {
                        response = getOrder(deposit.orderId(), accessToken);
                    } else { throw ex; }
                }
            }
            return verifyCapture(response, deposit);
        } catch (RestClientException ex) { throw providerError(ex); }
    }

    private Map<?, ?> getOrder(String id, String accessToken) {
        return paypal.get().uri("/v2/checkout/orders/{id}", id)
                .headers(h -> h.setBearerAuth(accessToken)).retrieve().body(Map.class);
    }

    static Capture verifyCapture(Map<?, ?> response, WalletLedger.Deposit deposit) {
        if (response == null || !deposit.orderId().equals(response.get("id"))) throw invalidResponse();
        String status = String.valueOf(response.get("status"));
        if (List.of("CREATED", "SAVED", "PAYER_ACTION_REQUIRED").contains(status)) {
            throw new PaymentException(HttpStatus.CONFLICT, "ORDER_NOT_APPROVED",
                    "PayPal pasūtījums nav apstiprināts. Vari mainīt summu un mēģināt vēlreiz.");
        }
        if (!"COMPLETED".equals(status)) throw pending();
        if (!(response.get("purchase_units") instanceof List<?> units) || units.size() != 1
                || !(units.get(0) instanceof Map<?, ?> unit)
                || !(unit.get("payments") instanceof Map<?, ?> payments)
                || !(payments.get("captures") instanceof List<?> captures) || captures.size() != 1
                || !(captures.get(0) instanceof Map<?, ?> capture)) throw invalidResponse();
        if ("PENDING".equals(capture.get("status"))) throw pending();
        if (!"COMPLETED".equals(capture.get("status"))) {
            throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_NOT_COMPLETED",
                    "PayPal maksājums nav pabeigts. Bilance nav papildināta.");
        }
        if (!(capture.get("id") instanceof String captureId) || captureId.isBlank()
                || captureId.length() > 64 || !(capture.get("amount") instanceof Map<?, ?> money)
                || !deposit.currency().equals(money.get("currency_code"))) throw invalidResponse();
        // Some API responses put custom_id on the purchase unit, others on the capture.
        for (Object customId : new Object[]{unit.get("custom_id"), capture.get("custom_id")}) {
            if (customId != null && !deposit.id().toString().equals(customId)) throw invalidResponse();
        }
        try {
            BigDecimal amount = new BigDecimal(String.valueOf(money.get("value")));
            if (amount.signum() <= 0 || amount.compareTo(deposit.amount()) != 0) throw invalidResponse();
            return new Capture(deposit.orderId(), captureId, deposit.amount(), deposit.currency());
        } catch (NumberFormatException ex) { throw invalidResponse(); }
    }

    private String token() {
        Map<?, ?> response = paypal.post().uri("/v1/oauth2/token")
                .headers(h -> h.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials").retrieve().body(Map.class);
        if (response == null || !(response.get("access_token") instanceof String token) || token.isBlank()) {
            throw invalidResponse();
        }
        return token;
    }

    private static PaymentException pending() {
        return new PaymentException(HttpStatus.CONFLICT, "PAYMENT_PENDING",
                "PayPal vēl apstrādā maksājumu. Neveic jaunu maksājumu; pārbaudi šo vēlreiz.");
    }

    private static PaymentException invalidResponse() {
        return new PaymentException(HttpStatus.BAD_GATEWAY, "CAPTURE_MISMATCH",
                "PayPal maksājuma dati nesakrīt vai nav pilnīgi. Bilance nav papildināta.");
    }

    private PaymentException providerError(RestClientException ex) {
        // Do not log credentials, access tokens or provider response bodies.
        System.err.println("PayPal error type: " + ex.getClass().getSimpleName());
        if (ex instanceof RestClientResponseException response)
            System.err.println("PayPal HTTP status: " + response.getStatusCode());
        return new PaymentException(HttpStatus.BAD_GATEWAY, "PAYPAL_UNAVAILABLE",
                "PayPal pieprasījums neizdevās. Mēģini pārbaudīt to pašu maksājumu vēlreiz.");
    }
}
