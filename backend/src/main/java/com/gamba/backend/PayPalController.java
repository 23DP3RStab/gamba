package com.gamba.backend;

import com.gamba.backend.wallet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

@RestController
@RequestMapping("/api/paypal")
public class PayPalController {
    private final PayPalGateway paypal;
    private final WalletLedger ledger;
    private final BrowserWallet browserWallet;
    private final WithdrawalLedger withdrawalLedger;
    private final WithdrawalService withdrawalService;
    private final String clientId;

    public record DepositRequest(
            @NotNull @DecimalMin("0.01") @DecimalMax("1000.00")
            @Digits(integer = 4, fraction = 2) BigDecimal amount,
            UUID requestId) {}

    public record WithdrawalRequest(
            @NotNull @DecimalMin("0.01") @DecimalMax("1000.00")
            @Digits(integer = 4, fraction = 2) BigDecimal amount,
            @NotBlank @Email @Size(max = 127) @Pattern(regexp = "\\p{ASCII}+") String recipient,
            @NotNull UUID requestId) {}

    public PayPalController(PayPalGateway paypal, WalletLedger ledger, BrowserWallet browserWallet,
                            WithdrawalLedger withdrawalLedger, WithdrawalService withdrawalService,
                            @Value("${paypal.client-id}") String clientId) {
        this.paypal = paypal;
        this.ledger = ledger;
        this.browserWallet = browserWallet;
        this.withdrawalLedger = withdrawalLedger;
        this.withdrawalService = withdrawalService;
        this.clientId = clientId;
    }

    @GetMapping("/config")
    public Map<String, String> config() {
        return Map.of("clientId", clientId, "environment", "sandbox");
    }

    @GetMapping("/status")
    public Map<String, String> status() {
        paypal.checkConnection();
        return Map.of("status", "connected", "environment", "sandbox");
    }

    @GetMapping("/wallet")
    public Map<String, String> wallet(HttpServletRequest request, HttpServletResponse response) {
        UUID id = browserWallet.resolve(request, response);
        return Map.of("balance", ledger.balance(id).toPlainString(), "currency", "EUR",
                "walletId", id.toString(), "environment", "sandbox", "reserved", withdrawalLedger.reserved(id).toPlainString());
    }

    @GetMapping("/withdrawals")
    public Map<String, Object> withdrawals(HttpServletRequest request, HttpServletResponse response) {
        UUID id = browserWallet.resolve(request, response);
        return Map.of("withdrawals", withdrawalLedger.recent(id), "balance", ledger.balance(id).toPlainString(),
                "reserved", withdrawalLedger.reserved(id).toPlainString());
    }

    @PostMapping(value = "/withdrawals", consumes = MediaType.APPLICATION_JSON_VALUE)
    public WithdrawalLedger.View withdraw(@Valid @RequestBody WithdrawalRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        rejectCrossSite(request);
        UUID id = browserWallet.resolve(request, response);
        return withdrawalService.request(id, body.requestId(), body.amount(), body.recipient());
    }

    @PostMapping(value = "/withdrawals/{id}/check", consumes = MediaType.APPLICATION_JSON_VALUE)
    public WithdrawalLedger.View checkWithdrawal(@PathVariable UUID id,
            HttpServletRequest request, HttpServletResponse response) {
        rejectCrossSite(request);
        return withdrawalService.check(browserWallet.resolve(request, response), id);
    }

    @PostMapping(value = "/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> createOrder(@Valid @RequestBody DepositRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        rejectCrossSite(request);
        UUID walletId = browserWallet.resolve(request, response);
        UUID requestId = body.requestId() == null ? UUID.randomUUID() : body.requestId();
        // Commit a local payment intent BEFORE contacting PayPal. Retries reuse its ID.
        WalletLedger.Deposit deposit = ledger.prepare(walletId, requestId, body.amount());
        if (deposit.orderId() == null) {
            String orderId = paypal.createOrder(deposit);
            deposit = ledger.attachOrder(walletId, deposit.id(), orderId);
        }
        return Map.of("id", deposit.orderId(), "status",
                deposit.creditedAt() == null ? "CREATED" : "COMPLETED");
    }

    @PostMapping(value = "/orders/{orderId}/capture", consumes = MediaType.APPLICATION_JSON_VALUE)
    public WalletLedger.Receipt capture(@PathVariable String orderId,
            HttpServletRequest request, HttpServletResponse response) {
        rejectCrossSite(request);
        if (!orderId.matches("[A-Z0-9]{1,36}")) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "INVALID_ORDER_ID", "Nederīgs pasūtījuma ID.");
        }
        UUID walletId = browserWallet.resolve(request, response);
        WalletLedger.Deposit deposit = ledger.findOrder(walletId, orderId);
        if (deposit.creditedAt() != null) return ledger.receipt(walletId, deposit.id());
        // Network calls stay outside the database transaction. Only confirmed captures may credit it.
        PayPalGateway.Capture capture = paypal.capture(deposit);
        return ledger.credit(walletId, deposit.id(), capture);
    }

    private void rejectCrossSite(HttpServletRequest request) {
        if ("cross-site".equals(request.getHeader("Sec-Fetch-Site"))) {
            throw new PaymentException(HttpStatus.FORBIDDEN, "CROSS_SITE_REQUEST", "Pieprasījums nav atļauts.");
        }
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Map<String, String>> paymentError(PaymentException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("message", ex.getMessage(), "code", ex.getCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validationError(MethodArgumentNotValidException ex) {
        if (ex.getBindingResult().hasFieldErrors("recipient")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ievadi derīgu PayPal testa konta e-pastu.", "code", "INVALID_EMAIL"));
        }
        return ResponseEntity.badRequest().body(Map.of("message",
                "Ievadi summu no €0.01 līdz €1000.00 ar ne vairāk kā divām decimālzīmēm.", "code", "INVALID_AMOUNT"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message",
                "Maksājuma ieraksts konfliktē. Pārbaudi to pašu maksājumu vēlreiz.", "code", "PAYMENT_CONFLICT"));
    }
}
