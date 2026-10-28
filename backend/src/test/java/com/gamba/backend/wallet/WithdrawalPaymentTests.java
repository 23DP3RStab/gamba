package com.gamba.backend.wallet;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(WithdrawalPaymentTests.Fakes.class)
class WithdrawalPaymentTests {
    @TestConfiguration(proxyBeanMethods = false)
    static class Fakes { @Bean @Primary FakePayouts fakePayouts() { return new FakePayouts(); } }
    static class FakePayouts implements PayoutGateway {
        final Map<UUID, String> modes = new ConcurrentHashMap<>();
        final Map<UUID, AtomicInteger> submissions = new ConcurrentHashMap<>();
        final Set<UUID> failChecks = ConcurrentHashMap.newKeySet();
        @Override public String submit(WithdrawalLedger.Withdrawal w) {
            submissions.computeIfAbsent(w.id(), k -> new AtomicInteger()).incrementAndGet();
            String mode = modes.getOrDefault(w.id(), "SUCCESS");
            if (mode.equals("REJECT")) throw new PayoutRejectedException("PAYOUT_ACCESS_DENIED", "No Payouts access");
            if (mode.equals("UNCERTAIN")) throw new PaymentException(HttpStatus.BAD_GATEWAY, "PAYOUT_UNCERTAIN", "Lost response");
            return batch(w.id());
        }
        @Override public Result check(WithdrawalLedger.Withdrawal w) {
            if (failChecks.contains(w.id())) throw new PaymentException(HttpStatus.BAD_GATEWAY, "PAYOUT_UNCERTAIN", "Check unavailable");
            return new Result(batch(w.id()), item(w.id()), "T" + compact(w.id()), modes.getOrDefault(w.id(), "SUCCESS"));
        }
        static String compact(UUID id) { return id.toString().replace("-", ""); }
        static String batch(UUID id) { return "B" + compact(id); }
        static String item(UUID id) { return "I" + compact(id); }
        int count(UUID id) { return submissions.getOrDefault(id, new AtomicInteger()).get(); }
    }
    @Autowired Environment environment;
    @Autowired WalletLedger deposits;
    @Autowired WithdrawalLedger ledger;
    @Autowired SandboxWithdrawalRepository records;
    @Autowired FakePayouts paypal;

    private record Browser(RestClient http, CookieManager cookies, UUID walletId) {}
    private Browser connection(CookieManager cookies, UUID id) {
        return new Browser(RestClient.builder().baseUrl("http://localhost:" + environment.getProperty("local.server.port"))
                .requestFactory(new JdkClientHttpRequestFactory(HttpClient.newBuilder().cookieHandler(cookies).build())).build(), cookies, id);
    }
    private Browser browser(String balance) {
        Browser b = connection(new CookieManager(null, CookiePolicy.ACCEPT_ALL), null);
        Map<?, ?> wallet = b.http().get().uri("/api/paypal/wallet").retrieve().body(Map.class);
        UUID id = UUID.fromString((String) wallet.get("walletId"));
        if (new BigDecimal(balance).signum() > 0) {
            UUID deposit = UUID.randomUUID(); String order = "O" + FakePayouts.compact(deposit);
            deposits.prepare(id, deposit, new BigDecimal(balance)); deposits.attachOrder(id, deposit, order);
            deposits.credit(id, deposit, new PayPalGateway.Capture(order, "C" + FakePayouts.compact(deposit), new BigDecimal(balance), "EUR"));
        }
        return new Browser(b.http(), b.cookies(), id);
    }
    private Map<?, ?> withdraw(Browser b, UUID id, String amount, String email) {
        return b.http().post().uri("/api/paypal/withdrawals").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("requestId", id.toString(), "amount", amount, "recipient", email)).retrieve().body(Map.class);
    }
    private Map<?, ?> check(Browser b, UUID id) {
        return b.http().post().uri("/api/paypal/withdrawals/{id}/check", id).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of()).retrieve().body(Map.class);
    }
    private void status(int expected, Runnable call) {
        RestClientResponseException error = assertThrows(RestClientResponseException.class, call::run);
        assertEquals(expected, error.getStatusCode().value());
    }
    @Test void successSubtractsOnceAndSavesPayoutDetails() {
        Browser b = browser("10.00"); UUID id = UUID.randomUUID();
        Map<?, ?> result = withdraw(b, id, "5.00", "buyer@personal.example.com");
        assertEquals("SUCCESS", result.get("status")); assertEquals("5.00", result.get("balance"));
        assertEquals("0.00", result.get("reserved")); assertNotNull(result.get("transactionId"));
        assertEquals(FakePayouts.batch(id), records.findById(id).orElseThrow().getPayoutBatchId());
        assertEquals("5.00", withdraw(b, id, "5.00", "buyer@personal.example.com").get("balance"));
        assertEquals(1, paypal.count(id));
    }
    @Test void pendingFundsAreUnavailableUntilTheIndividualPayoutCompletes() {
        Browser b = browser("10.00"); UUID id = UUID.randomUUID(); paypal.modes.put(id, "PENDING");
        Map<?, ?> pending = withdraw(b, id, "5.00", "buyer@personal.example.com");
        assertEquals("PROCESSING", pending.get("status")); assertEquals("5.00", pending.get("balance"));
        assertEquals("5.00", pending.get("reserved"));
        status(400, () -> withdraw(b, UUID.randomUUID(), "6.00", "buyer@personal.example.com"));
        paypal.modes.put(id, "SUCCESS");
        assertEquals("SUCCESS", check(b, id).get("status")); assertEquals("0.00", check(b, id).get("reserved"));
        assertEquals(1, paypal.count(id));
    }
    @Test void insufficientFundsNeverCallsPayPalOrCreatesARecord() {
        Browser b = browser("10.00"); UUID id = UUID.randomUUID();
        status(400, () -> withdraw(b, id, "15.00", "buyer@personal.example.com"));
        assertEquals(0, paypal.count(id)); assertFalse(records.existsById(id));
        assertEquals(new BigDecimal("10.00"), deposits.balance(b.walletId()));
    }
    @Test void invalidAmountsAndEmailsDoNotReserveFunds() {
        Browser b = browser("10.00");
        for (String amount : List.of("0", "-1", "1000.01", "5.001", "abc")) {
            UUID id = UUID.randomUUID(); status(400, () -> withdraw(b, id, amount, "buyer@personal.example.com"));
            assertFalse(records.existsById(id));
        }
        UUID id = UUID.randomUUID(); status(400, () -> withdraw(b, id, "5.00", "abc"));
        assertFalse(records.existsById(id)); assertEquals(new BigDecimal("10.00"), deposits.balance(b.walletId()));
    }
    @Test void definiteFirstRejectionReturnsFundsWithoutResubmittingTheRequest() {
        Browser b = browser("10.00"); UUID id = UUID.randomUUID(); paypal.modes.put(id, "REJECT");
        Map<?, ?> result = withdraw(b, id, "5.00", "buyer@personal.example.com");
        assertEquals("FAILED", result.get("status")); assertEquals("10.00", result.get("balance"));
        assertEquals("PAYOUT_ACCESS_DENIED", result.get("errorCode")); assertEquals("0.00", result.get("reserved"));
        check(b, id); withdraw(b, id, "5.00", "buyer@personal.example.com"); assertEquals(1, paypal.count(id));
    }
    @Test void processingFailureReturnsFundsOnlyOnce() {
        Browser b = browser("10.00"); UUID id = UUID.randomUUID(); paypal.modes.put(id, "PENDING");
        withdraw(b, id, "5.00", "buyer@personal.example.com"); paypal.modes.put(id, "FAILED");
        assertEquals("10.00", check(b, id).get("balance")); assertEquals("10.00", check(b, id).get("balance"));
        assertEquals("0.00", ledger.view(b.walletId(), id).reserved());
    }
    @Test void returnedPayoutAfterSuccessRestoresFundsOnlyOnce() {
        Browser b = browser("10.00"); UUID id = UUID.randomUUID();
        withdraw(b, id, "5.00", "buyer@personal.example.com"); paypal.modes.put(id, "RETURNED");
        assertEquals("RETURNED", check(b, id).get("status"));
        assertEquals("10.00", check(b, id).get("balance")); assertEquals(1, paypal.count(id));
    }
    @Test void unclaimedOnHoldAndBlockedPayoutsKeepFundsReserved() {
        for (String providerStatus : List.of("UNCLAIMED", "ONHOLD", "BLOCKED")) {
            Browser b = browser("10.00"); UUID id = UUID.randomUUID(); paypal.modes.put(id, providerStatus);
            Map<?, ?> result = withdraw(b, id, "5.00", "buyer@personal.example.com");
            assertEquals("PROCESSING", result.get("status")); assertEquals("5.00", result.get("reserved"));
        }
    }
    @Test void uncertainSubmissionRecoversTheSameProviderBatch() {
        Browser b = browser("10.00"); UUID id = UUID.randomUUID(); paypal.modes.put(id, "UNCERTAIN");
        status(502, () -> withdraw(b, id, "5.00", "buyer@personal.example.com"));
        assertEquals("5.00", ledger.view(b.walletId(), id).balance());
        paypal.modes.put(id, "SUCCESS");
        assertEquals("SUCCESS", check(b, id).get("status")); assertEquals("5.00", check(b, id).get("balance"));
        assertEquals(2, paypal.count(id)); assertEquals(FakePayouts.batch(id), ledger.find(b.walletId(), id).batchId());
    }
    @Test void laterRejectionCannotReleaseAnEarlierUncertainPayout() {
        Browser b = browser("10.00"); UUID id = UUID.randomUUID(); paypal.modes.put(id, "UNCERTAIN");
        status(502, () -> withdraw(b, id, "5.00", "buyer@personal.example.com")); paypal.modes.put(id, "REJECT");
        Map<?, ?> result = check(b, id);
        assertEquals("REQUESTED", result.get("status")); assertEquals("5.00", result.get("reserved"));
        assertEquals("5.00", result.get("balance"));
    }
    @Test void lostStatusResponseDoesNotResendAnAcceptedPayout() {
        Browser b = browser("10.00"); UUID id = UUID.randomUUID(); paypal.failChecks.add(id);
        status(502, () -> withdraw(b, id, "5.00", "buyer@personal.example.com"));
        assertNotNull(records.findById(id).orElseThrow().getPayoutBatchId()); paypal.failChecks.remove(id);
        assertEquals("SUCCESS", check(b, id).get("status")); assertEquals(1, paypal.count(id));
    }
    @Test void anotherBrowserCannotInspectOrRetryAWithdrawal() {
        Browser owner = browser("10.00"); UUID id = UUID.randomUUID();
        withdraw(owner, id, "5.00", "buyer@personal.example.com"); Browser other = browser("0.00");
        status(404, () -> check(other, id));
        Map<?, ?> list = other.http().get().uri("/api/paypal/withdrawals").retrieve().body(Map.class);
        assertTrue(((List<?>) list.get("withdrawals")).isEmpty());
    }
    @Test void requestIdCannotBeReusedForAnotherAmountOrRecipient() {
        Browser b = browser("10.00"); UUID id = UUID.randomUUID();
        withdraw(b, id, "5.00", "buyer@personal.example.com");
        status(409, () -> withdraw(b, id, "6.00", "buyer@personal.example.com"));
        status(409, () -> withdraw(b, id, "5.00", "another@personal.example.com")); assertEquals(1, paypal.count(id));
    }
    @Test void stalePendingOrFailedResponsesCannotUndoSuccess() {
        Browser b = browser("10.00"); UUID id = UUID.randomUUID(); withdraw(b, id, "5.00", "buyer@personal.example.com");
        for (String state : List.of("PENDING", "FAILED")) ledger.apply(b.walletId(), id,
                new PayoutGateway.Result(FakePayouts.batch(id), FakePayouts.item(id), null, state));
        assertEquals("SUCCESS", ledger.find(b.walletId(), id).status()); assertEquals("5.00", ledger.view(b.walletId(), id).balance());
    }
    @Test void twoConcurrentWithdrawalsCannotOverdrawTheWallet() throws Exception {
        Browser b = browser("10.00"); UUID one = UUID.randomUUID(), two = UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(2); CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (UUID id : List.of(one, two)) futures.add(executor.submit(() -> {
                start.await(); try { withdraw(b, id, "7.00", "buyer@personal.example.com"); return true; }
                catch (RestClientResponseException ex) { assertEquals(400, ex.getStatusCode().value()); return false; }
            }));
            start.countDown(); assertNotEquals(futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS));
            assertEquals(new BigDecimal("3.00"), deposits.balance(b.walletId()));
        } finally { executor.shutdownNow(); }
    }
    @Test void aSecondSubmissionCannotClaimAnActiveLease() {
        Browser b = browser("10.00"); UUID id = UUID.randomUUID();
        ledger.reserve(b.walletId(), id, new BigDecimal("5.00"), "buyer@personal.example.com");
        assertTrue(ledger.claimSubmission(b.walletId(), id).claimed()); assertFalse(ledger.claimSubmission(b.walletId(), id).claimed());
        assertEquals(1, ledger.find(b.walletId(), id).submitAttempts()); assertEquals(new BigDecimal("5.00"), deposits.balance(b.walletId()));
    }
    @Test void newConnectionSeesSavedPendingWithdrawalAndReservation() {
        Browser b = browser("10.00"); UUID id = UUID.randomUUID(); paypal.modes.put(id, "PENDING");
        withdraw(b, id, "5.00", "buyer@personal.example.com");
        Map<?, ?> result = connection(b.cookies(), b.walletId()).http().get().uri("/api/paypal/withdrawals").retrieve().body(Map.class);
        assertEquals("5.00", result.get("balance")); assertEquals("5.00", result.get("reserved"));
        assertEquals(id.toString(), ((Map<?, ?>)((List<?>)result.get("withdrawals")).get(0)).get("id"));
    }

    @Test void pendingPayoutRemainsVisibleAfterMoreThanTenNewerWithdrawals() {
        Browser b = browser("100.00"); UUID pending = UUID.randomUUID(); paypal.modes.put(pending, "PENDING");
        withdraw(b, pending, "5.00", "buyer@personal.example.com");
        for (int i = 0; i < 11; i++) withdraw(b, UUID.randomUUID(), "1.00", "buyer@personal.example.com");
        assertTrue(ledger.recent(b.walletId()).stream().anyMatch(w -> w.id().equals(pending.toString())));
        assertEquals("5.00", ledger.view(b.walletId(), pending).reserved());
    }
}
