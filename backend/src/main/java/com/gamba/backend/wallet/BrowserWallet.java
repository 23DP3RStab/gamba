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
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE.equals(cookie.getName()) && cookie.getValue().matches("[A-Za-z0-9_-]{43}")) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        if (token == null) {
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }
        UUID id = ledger.resolveWallet(hash(token));
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(COOKIE, token)
                .httpOnly(true).secure(request.isSecure()).sameSite("Lax").path("/api")
                .maxAge(Duration.ofDays(365)).build().toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return id;
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
