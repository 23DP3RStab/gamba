package com.gamba.backend.wallet;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** A random persistent browser credential for LOCAL Sandbox use, not a user login. */
@Component
public class BrowserWallet {
    private static final String COOKIE = "gamba_sandbox_wallet";
    private final SecureRandom random = new SecureRandom();
    private final WalletLedger ledger;

    public BrowserWallet(WalletLedger ledger) { this.ledger = ledger; }

    public UUID resolve(HttpServletRequest request, HttpServletResponse response) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof com.gamba.backend.model.User user)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Log in to use the wallet.");
        }
        // One payment-record wallet per account, never per browser/cookie.
        return ledger.resolveWallet("user:" + user.getId());
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
