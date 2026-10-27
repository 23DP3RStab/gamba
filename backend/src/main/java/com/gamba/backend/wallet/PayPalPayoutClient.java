package com.gamba.backend.wallet;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

@Service
public class PayPalPayoutClient implements PayoutGateway {
    private final RestClient paypal;
    private final String clientId;
    private final String secret;
    private static final Set<String> REJECTIONS = Set.of("INSUFFICIENT_FUNDS", "AUTHORIZATION_ERROR",
            "REQUIRED_SCOPE_MISSING", "PERMISSION_DENIED", "SENDER_ACCOUNT_UNVERIFIED", "SENDER_EMAIL_UNCONFIRMED",
            "INVALID_EMAIL", "VALIDATION_ERROR", "MALFORMED_REQUEST", "SELF_PAY_NOT_ALLOWED", "USER_COUNTRY_NOT_ALLOWED",
            "SENDER_ACCOUNT_RESTRICTED", "ACCOUNT_RESTRICTED", "PAYOUT_NOT_AVAILABLE", "CLOSED_MARKET");

    @Autowired
    public PayPalPayoutClient(@Value("${paypal.client-id}") String id, @Value("${paypal.client-secret}") String secret) {
        this.clientId = id; this.secret = secret;
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(20));
        paypal = RestClient.builder().baseUrl("https://api-m.sandbox.paypal.com").requestFactory(factory).build();
    }
    PayPalPayoutClient(RestClient paypal, String id, String secret) {
        this.paypal = paypal; this.clientId = id; this.secret = secret;
    }

    @Override
    public String submit(WithdrawalLedger.Withdrawal w) {
        String token = token();
        try {
            Map<String, Object> body = Map.of("sender_batch_header", Map.of(
                    "sender_batch_id", w.id().toString(), "email_subject", "Gamba Sandbox withdrawal"),
                    "items", List.of(Map.of("recipient_type", "EMAIL", "recipient_wallet", "PAYPAL",
                            "receiver", w.recipient(), "sender_item_id", w.id().toString(),
                            "amount", Map.of("currency", w.currency(), "value", w.amount().toPlainString()),
                            "note", "Gamba Sandbox test withdrawal")));
            Map<?, ?> response = paypal.post().uri("/v1/payments/payouts")
                    .headers(h -> { h.setBearerAuth(token); h.set("PayPal-Request-Id", w.id().toString()); })
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class);
            if (response == null || !(response.get("batch_header") instanceof Map<?, ?> header)) throw mismatch();
            checkSender(header, w);
            return id(header.get("payout_batch_id"));
        } catch (RestClientResponseException ex) {
            Map<?, ?> body = errorBody(ex);
            if (containsCode(body, "DUPLICATE_BATCH_ID")) {
                String duplicateId = duplicateBatch(body, 0);
                if (duplicateId != null) return duplicateId;
                throw unavailable("PAYOUT_REVIEW_REQUIRED");
            }
            String name = String.valueOf(body.get("name"));
            System.err.println("PayPal Payouts HTTP status: " + ex.getStatusCode());
            if (Set.of(400, 401, 403, 422).contains(ex.getStatusCode().value()) && REJECTIONS.contains(name)) {
                String code = name.equals("INSUFFICIENT_FUNDS") ? "PAYOUT_INSUFFICIENT_FUNDS"
                        : Set.of("AUTHORIZATION_ERROR", "REQUIRED_SCOPE_MISSING", "PERMISSION_DENIED",
                                "PAYOUT_NOT_AVAILABLE").contains(name) ? "PAYOUT_ACCESS_DENIED" : "PAYOUT_REJECTED";
                throw new PayoutRejectedException(code, "PayPal noraidīja izmaksas pieprasījumu.");
            }
            throw unavailable("PAYOUT_UNCERTAIN");
        } catch (RestClientException ex) { throw unavailable("PAYOUT_UNCERTAIN"); }
    }

    @Override
    public Result check(WithdrawalLedger.Withdrawal w) {
        try {
            String token = token();
            Map<?, ?> response = paypal.get().uri("/v1/payments/payouts/{id}", w.batchId())
                    .headers(h -> h.setBearerAuth(token)).retrieve().body(Map.class);
            return verify(response, w);
        } catch (RestClientException ex) { throw unavailable("PAYOUT_UNCERTAIN"); }
    }

    static Result verify(Map<?, ?> response, WithdrawalLedger.Withdrawal w) {
        if (response == null || !(response.get("batch_header") instanceof Map<?, ?> header)) throw mismatch();
        String batchId = id(header.get("payout_batch_id"));
        if (!batchId.equals(w.batchId())) throw mismatch();
        checkSender(header, w);
        String batchStatus = String.valueOf(header.get("batch_status"));
        Object rawItems = response.get("items");
        if (rawItems instanceof List<?> items && items.size() == 1) {
            if (!(items.get(0) instanceof Map<?, ?> item) || !(item.get("payout_item") instanceof Map<?, ?> payout)) throw mismatch();
            String itemId = id(item.get("payout_item_id"));
            if (!w.id().toString().equals(payout.get("sender_item_id"))
                    || !"EMAIL".equals(payout.get("recipient_type"))
                    || !(payout.get("receiver") instanceof String recipient)
                    || !w.recipient().equalsIgnoreCase(recipient.trim())
                    || !(payout.get("amount") instanceof Map<?, ?> money)
                    || !w.currency().equals(money.get("currency"))) throw mismatch();
            if (item.get("payout_batch_id") != null && !batchId.equals(item.get("payout_batch_id"))) throw mismatch();
            if (payout.get("recipient_wallet") != null && !"PAYPAL".equals(payout.get("recipient_wallet"))) throw mismatch();
            try {
                if (new BigDecimal(String.valueOf(money.get("value"))).compareTo(w.amount()) != 0) throw mismatch();
            } catch (NumberFormatException ex) { throw mismatch(); }
            String status = String.valueOf(item.get("transaction_status"));
            if (!Set.of("SUCCESS", "FAILED", "PENDING", "UNCLAIMED", "RETURNED", "ONHOLD", "BLOCKED",
                    "REFUNDED", "REVERSED").contains(status)) throw mismatch();
            String transactionId = item.get("transaction_id") instanceof String tx && !tx.isBlank() ? id(tx) : null;
            if (status.equals("SUCCESS") && transactionId == null) throw mismatch();
            return new Result(batchId, itemId, transactionId, status);
        }
        if (rawItems instanceof List<?> items && !items.isEmpty()) throw mismatch();
        // Batch SUCCESS alone is not proof that its recipient has been paid.
        if (Set.of("DENIED", "CANCELED").contains(batchStatus)) return new Result(batchId, null, null, batchStatus);
        return new Result(batchId, null, null, "PENDING");
    }

    private String token() {
        try {
            Map<?, ?> response = paypal.post().uri("/v1/oauth2/token").headers(h -> h.setBasicAuth(clientId, secret))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body("grant_type=client_credentials")
                    .retrieve().body(Map.class);
            if (response == null || !(response.get("access_token") instanceof String token) || token.isBlank()) throw unavailable("PAYOUT_UNCERTAIN");
            return token;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403)
                throw new PayoutRejectedException("PAYOUT_ACCESS_DENIED", "PayPal noraidīja Sandbox akreditācijas datus.");
            throw unavailable("PAYOUT_UNCERTAIN");
        } catch (RestClientException ex) { throw unavailable("PAYOUT_UNCERTAIN"); }
    }
    private static void checkSender(Map<?, ?> header, WithdrawalLedger.Withdrawal w) {
        Object sender = header.get("sender_batch_header");
        if (!(sender instanceof Map<?, ?> map) || !w.id().toString().equals(map.get("sender_batch_id"))) throw mismatch();
    }
    private static String id(Object value) {
        if (!(value instanceof String id) || !id.matches("[A-Za-z0-9]{1,64}")) throw mismatch();
        return id;
    }
    private static PaymentException mismatch() {
        return new PaymentException(HttpStatus.BAD_GATEWAY, "PAYOUT_MISMATCH",
                "PayPal izmaksas dati nesakrīt. Summa paliek rezervēta līdz pārbaudei.");
    }
    private static PaymentException unavailable(String code) {
        return new PaymentException(HttpStatus.BAD_GATEWAY, code,
                "Izmaksas statuss nav apstiprināts. Pārbaudi to pašu pieprasījumu vēlreiz; nesūti jaunu.");
    }
    private Map<?, ?> errorBody(RestClientResponseException ex) {
        try { Map<?, ?> body = ex.getResponseBodyAs(Map.class); return body == null ? Map.of() : body; }
        catch (RuntimeException ignored) { return Map.of(); }
    }
    private boolean containsCode(Object value, String code) {
        if (value instanceof Map<?, ?> map) return map.values().stream().anyMatch(v -> containsCode(v, code));
        if (value instanceof List<?> list) return list.stream().anyMatch(v -> containsCode(v, code));
        return code.equals(value);
    }
    private String duplicateBatch(Object node, int depth) {
        if (depth > 6) return null;
        if (node instanceof Map<?, ?> map) {
            if (map.get("href") instanceof String href) {
                try {
                    URI uri = URI.create(href);
                    if ("https".equals(uri.getScheme()) && uri.getHost() != null && Set.of("api-m.sandbox.paypal.com", "api.sandbox.paypal.com").contains(uri.getHost())
                            && uri.getUserInfo() == null && uri.getPort() == -1
                            && uri.getPath().matches("/v1/payments/payouts/[A-Za-z0-9]{1,64}")) {
                        return uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);
                    }
                } catch (IllegalArgumentException ignored) { /* Do not follow invalid URLs. */ }
            }
            for (Object child : map.values()) { String found = duplicateBatch(child, depth + 1); if (found != null) return found; }
        } else if (node instanceof List<?> list) {
            for (Object child : list) { String found = duplicateBatch(child, depth + 1); if (found != null) return found; }
        }
        return null;
    }
}
