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

class WithdrawalRestartTests {
    @TempDir Path directory;
    @Test void pendingReservationAndPayoutIdsSurviveRestartAndSettleOnlyOnce() {
        UUID id = UUID.randomUUID(); UUID walletId;
        String[] args = {"--spring.main.web-application-type=none", "--logging.level.root=ERROR",
                "--spring.datasource.url=jdbc:h2:file:" + directory.resolve("withdrawals") + ";MODE=PostgreSQL",
                "--spring.jpa.hibernate.ddl-auto=update"};
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(BackendApplication.class).run(args)) {
            WalletLedger deposits = context.getBean(WalletLedger.class);
            walletId = deposits.resolveWallet("b".repeat(64));
            UUID seed = UUID.randomUUID(); deposits.prepare(walletId, seed, new BigDecimal("10.00"));
            deposits.attachOrder(walletId, seed, "SEEDORDER");
            deposits.credit(walletId, seed, new PayPalGateway.Capture("SEEDORDER", "SEEDCAPTURE", new BigDecimal("10.00"), "EUR"));
            WithdrawalLedger ledger = context.getBean(WithdrawalLedger.class);
            ledger.reserve(walletId, id, new BigDecimal("5.00"), "buyer@personal.example.com");
            ledger.attachBatch(walletId, id, "BATCH1");
            assertEquals("5.00", ledger.view(walletId, id).reserved());
        }
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(BackendApplication.class).run(args)) {
            WithdrawalLedger ledger = context.getBean(WithdrawalLedger.class);
            assertEquals("BATCH1", ledger.find(walletId, id).batchId());
            assertEquals("5.00", ledger.view(walletId, id).balance());
            assertEquals("5.00", ledger.view(walletId, id).reserved());
            ledger.apply(walletId, id, new PayoutGateway.Result("BATCH1", "ITEM1", "TX1", "SUCCESS"));
            ledger.apply(walletId, id, new PayoutGateway.Result("BATCH1", "ITEM1", "TX1", "SUCCESS"));
            assertEquals("5.00", ledger.view(walletId, id).balance());
            assertEquals("0.00", ledger.view(walletId, id).reserved());
            ledger.apply(walletId, id, new PayoutGateway.Result("BATCH1", "ITEM1", "TX1", "RETURNED"));
            ledger.apply(walletId, id, new PayoutGateway.Result("BATCH1", "ITEM1", "TX1", "RETURNED"));
            assertEquals("10.00", ledger.view(walletId, id).balance());
        }
    }
}
