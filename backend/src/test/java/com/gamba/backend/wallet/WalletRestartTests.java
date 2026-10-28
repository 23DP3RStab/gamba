package com.gamba.backend.wallet;

import static org.junit.jupiter.api.Assertions.*;
import com.gamba.backend.BackendApplication;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class WalletRestartTests {
    @TempDir Path directory;

    @Test void balanceReceiptAndExactlyOnceCreditSurviveAnApplicationRestart() {
        UUID requestId = UUID.randomUUID();
        UUID walletId;
        String hash = "a".repeat(64);
        String[] args = {
            "--spring.main.web-application-type=none",
            "--spring.datasource.url=jdbc:h2:file:" + directory.resolve("wallet") + ";MODE=PostgreSQL",
            "--spring.jpa.hibernate.ddl-auto=update",
            "--logging.level.root=ERROR"
        };
        try (ConfigurableApplicationContext first = new SpringApplicationBuilder(BackendApplication.class).run(args)) {
            WalletLedger ledger = first.getBean(WalletLedger.class);
            walletId = ledger.resolveWallet(hash);
            ledger.prepare(walletId, requestId, new BigDecimal("5.00"));
            ledger.attachOrder(walletId, requestId, "RESTARTORDER1");
            ledger.credit(walletId, requestId, new PayPalGateway.Capture(
                    "RESTARTORDER1", "RESTARTCAPTURE1", new BigDecimal("5.00"), "EUR"));
        }
        try (ConfigurableApplicationContext second = new SpringApplicationBuilder(BackendApplication.class).run(args)) {
            WalletLedger ledger = second.getBean(WalletLedger.class);
            assertEquals(walletId, ledger.resolveWallet(hash));
            assertEquals(new BigDecimal("5.00"), ledger.balance(walletId));
            WalletLedger.Deposit saved = ledger.findOrder(walletId, "RESTARTORDER1");
            assertNotNull(saved.creditedAt());
            assertEquals("RESTARTCAPTURE1", saved.captureId());
            assertEquals("5.00", ledger.credit(walletId, requestId, new PayPalGateway.Capture(
                    "RESTARTORDER1", "RESTARTCAPTURE1", new BigDecimal("5.00"), "EUR")).balance());
        }
    }
}
