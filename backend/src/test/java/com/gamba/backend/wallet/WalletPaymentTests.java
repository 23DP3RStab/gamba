package com.gamba.backend.wallet;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.http.HttpClient;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.*;

/** Real HTTP -> controller -> JPA -> H2 transactions. Only the external PayPal boundary is fake. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(WalletPaymentTests.FakeConfig.class)
class WalletPaymentTests {
    @TestConfiguration(proxyBeanMethods = false)
    static class FakeConfig {
        @Bean @Primary FakePayPal fakePayPal() { return new FakePayPal(); }
    }

    static class FakePayPal implements PayPalGateway {
        final Map<String, WalletLedger.Deposit> orders = new ConcurrentHashMap<>();
        final Set<String> pending = ConcurrentHashMap.newKeySet();
        final Set<String> notApproved = ConcurrentHashMap.newKeySet();
        final Map<String, String> captureIds = new ConcurrentHashMap<>();
        final AtomicBoolean failNextCreate = new AtomicBoolean();
        @Override public void checkConnection() {}
        @Override public String createOrder(WalletLedger.Deposit deposit) {
            String id = "O" + deposit.id().toString().replace("-", "").toUpperCase();
            orders.putIfAbsent(id, deposit);
            if (failNextCreate.getAndSet(false)) throw new PaymentException(
                    HttpStatus.BAD_GATEWAY, "PAYPAL_UNAVAILABLE", "Simulated lost create response");
            return id;
        }
        @Override public Capture capture(WalletLedger.Deposit deposit) {
            if (pending.contains(deposit.orderId())) throw new PaymentException(
                    HttpStatus.CONFLICT, "PAYMENT_PENDING", "Simulated pending payment");
            if (notApproved.contains(deposit.orderId())) throw new PaymentException(
                    HttpStatus.CONFLICT, "ORDER_NOT_APPROVED", "Simulated unapproved order");
            return new Capture(deposit.orderId(), captureIds.getOrDefault(deposit.orderId(),
                    "C" + deposit.id().toString().replace("-", "")), deposit.amount(), "EUR");
        }
    }

    @Autowired Environment environment;
    @Autowired WalletLedger ledger;
    @Autowired SandboxDepositRepository deposits;
    @Autowired FakePayPal paypal;

    private record Browser(RestClient client, CookieManager cookies) {}

    private Browser browser() {
        return browser(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
    }

    private Browser browser(CookieManager cookies) {
        HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).build();
        return new Browser(RestClient.builder()
                .baseUrl("http://localhost:" + environment.getProperty("local.server.port"))
                .requestFactory(new JdkClientHttpRequestFactory(http)).build(), cookies);
    }

    private Map<?, ?> balance(Browser browser) {
        return browser.client().get().uri("/api/paypal/wallet").retrieve().body(Map.class);
    }

    private Map<?, ?> create(Browser browser, UUID key, String amount) {
        return browser.client().post().uri("/api/paypal/orders").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amount, "requestId", key.toString())).retrieve().body(Map.class);
    }

    private Map<?, ?> capture(Browser browser, String orderId) {
        return browser.client().post().uri("/api/paypal/orders/{id}/capture", orderId)
                .contentType(MediaType.APPLICATION_JSON).body(Map.of()).retrieve().body(Map.class);
    }

    private void expectStatus(int expected, Runnable action) {
        RestClientResponseException failure = assertThrows(RestClientResponseException.class, action::run);
        assertEquals(expected, failure.getStatusCode().value());
    }

    @Test void newWalletHasZeroBalanceAndPrivatePersistentCookie() {
        Browser browser = browser();
        Map<?, ?> wallet = balance(browser);
        assertEquals("0.00", wallet.get("balance"));
        assertEquals("EUR", wallet.get("currency"));
        HttpCookie identity = browser.cookies().getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals("gamba_sandbox_wallet")).findFirst().orElseThrow();
        assertTrue(identity.isHttpOnly());
        assertTrue(identity.getMaxAge() > 0);
        assertEquals(43, identity.getValue().length());
    }

    @Test void completedDepositIsStoredAndVisibleFromANewConnection() {
        Browser first = browser();
        Map<?, ?> wallet = balance(first);
        UUID key = UUID.randomUUID();
        String id = (String) create(first, key, "5.00").get("id");
        Map<?, ?> receipt = capture(first, id);
        assertEquals("5.00", receipt.get("balance"));
        assertEquals("5.00", balance(browser(first.cookies())).get("balance"));
        SandboxDeposit saved = deposits.findById(key).orElseThrow();
        assertNotNull(saved.getCreditedAt());
        assertEquals(receipt.get("captureId"), saved.getCaptureId());
        assertEquals(new BigDecimal("5.00"), ledger.balance(UUID.fromString((String) wallet.get("walletId"))));
    }

    @Test void repeatedCaptureCreditsOnlyOnce() {
        Browser browser = browser(); balance(browser);
        String id = (String) create(browser, UUID.randomUUID(), "5.00").get("id");
        Map<?, ?> first = capture(browser, id);
        Map<?, ?> second = capture(browser, id);
        assertEquals(first.get("captureId"), second.get("captureId"));
        assertEquals("5.00", second.get("balance"));
        assertEquals("5.00", balance(browser).get("balance"));
    }

    @Test void differentBrowserCannotReadOrCaptureSomeoneElsesOrder() {
        Browser owner = browser(); balance(owner);
        String id = (String) create(owner, UUID.randomUUID(), "5.00").get("id");
        Browser other = browser();
        expectStatus(404, () -> capture(other, id));
        assertEquals("0.00", balance(other).get("balance"));
        assertEquals("0.00", balance(owner).get("balance"));
    }

    @Test void pendingPaymentDoesNotCreditButRetryCanCompleteIt() {
        Browser browser = browser(); balance(browser);
        String id = (String) create(browser, UUID.randomUUID(), "5.00").get("id");
        paypal.pending.add(id);
        expectStatus(409, () -> capture(browser, id));
        assertEquals("0.00", balance(browser).get("balance"));
        paypal.pending.remove(id);
        assertEquals("5.00", capture(browser, id).get("balance"));
    }

    @Test void unapprovedOrderDoesNotCreditWallet() {
        Browser browser = browser(); balance(browser);
        String id = (String) create(browser, UUID.randomUUID(), "5.00").get("id");
        paypal.notApproved.add(id);
        expectStatus(409, () -> capture(browser, id));
        assertEquals("0.00", balance(browser).get("balance"));
    }

    @Test void createRetriesReuseStoredIntentAndRejectChangedAmounts() {
        Browser browser = browser(); balance(browser);
        UUID key = UUID.randomUUID();
        String id = (String) create(browser, key, "5.00").get("id");
        assertEquals(id, create(browser, key, "5.00").get("id"));
        expectStatus(409, () -> create(browser, key, "6.00"));
    }

    @Test void createTimeoutCanRecoverUsingTheSameRequestId() {
        Browser browser = browser(); balance(browser);
        UUID key = UUID.randomUUID();
        paypal.failNextCreate.set(true);
        expectStatus(502, () -> create(browser, key, "5.00"));
        assertNull(deposits.findById(key).orElseThrow().getPaypalOrderId());
        String id = (String) create(browser, key, "5.00").get("id");
        assertEquals("O" + key.toString().replace("-", "").toUpperCase(), id);
        assertEquals("5.00", capture(browser, id).get("balance"));
    }

    @Test void duplicateProviderCaptureIdRollsBackBothCreditAndPaymentMark() {
        Browser browser = browser(); balance(browser);
        String first = (String) create(browser, UUID.randomUUID(), "5.00").get("id");
        UUID secondKey = UUID.randomUUID();
        String second = (String) create(browser, secondKey, "5.00").get("id");
        paypal.captureIds.put(first, "SAME" + UUID.randomUUID());
        paypal.captureIds.put(second, paypal.captureIds.get(first));
        capture(browser, first);
        expectStatus(409, () -> capture(browser, second));
        assertEquals("5.00", balance(browser).get("balance"));
        assertNull(deposits.findById(secondKey).orElseThrow().getCreditedAt());
    }

    @Test void concurrentConfirmationsDoNotDoubleCredit() throws Exception {
        Browser browser = browser(); balance(browser);
        String id = (String) create(browser, UUID.randomUUID(), "5.00").get("id");
        parallel(() -> capture(browser, id), () -> capture(browser, id));
        assertEquals("5.00", balance(browser).get("balance"));
    }

    @Test void concurrentDifferentPaymentsDoNotLoseABalanceUpdate() throws Exception {
        Browser browser = browser(); balance(browser);
        String first = (String) create(browser, UUID.randomUUID(), "5.00").get("id");
        String second = (String) create(browser, UUID.randomUUID(), "10.00").get("id");
        parallel(() -> capture(browser, first), () -> capture(browser, second));
        assertEquals("15.00", balance(browser).get("balance"));
    }

    private void parallel(Runnable a, Runnable b) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch gate = new CountDownLatch(1);
        try {
            Future<?> first = executor.submit(() -> { await(gate); a.run(); });
            Future<?> second = executor.submit(() -> { await(gate); b.run(); });
            gate.countDown();
            first.get(20, TimeUnit.SECONDS); second.get(20, TimeUnit.SECONDS);
        } finally { executor.shutdownNow(); }
    }

    private void await(CountDownLatch latch) {
        try { latch.await(); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new RuntimeException(ex); }
    }

    @Test void invalidAmountsAreRejectedByTheServer() {
        Browser browser = browser(); balance(browser);
        for (String amount : List.of("0", "-1", "1000.01", "10.001", "abc")) {
            expectStatus(400, () -> create(browser, UUID.randomUUID(), amount));
        }
        expectStatus(400, () -> browser.client().post().uri("/api/paypal/orders")
                .contentType(MediaType.APPLICATION_JSON).body("{\"amount\":null}").retrieve().body(Map.class));
        assertEquals("0.00", balance(browser).get("balance"));
    }

    @Test void validBoundaryAmountsCanCreateOrders() {
        Browser browser = browser(); balance(browser);
        assertNotNull(create(browser, UUID.randomUUID(), "0.01").get("id"));
        assertNotNull(create(browser, UUID.randomUUID(), "1000.00").get("id"));
    }

    @Test void crossSitePaymentRequestIsRejected() {
        Browser browser = browser(); balance(browser);
        expectStatus(403, () -> browser.client().post().uri("/api/paypal/orders")
                .header("Sec-Fetch-Site", "cross-site").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", "5.00")).retrieve().body(Map.class));
    }
}
