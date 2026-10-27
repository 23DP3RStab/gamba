package com.gamba.backend.wallet;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SandboxWithdrawalRepository extends JpaRepository<SandboxWithdrawal, UUID> {
    List<SandboxWithdrawal> findTop10ByWalletIdOrderByCreatedAtDesc(UUID walletId);
    List<SandboxWithdrawal> findByWalletIdAndStatusInOrderByCreatedAtDesc(UUID walletId, List<String> statuses);
    @Query("select coalesce(sum(w.amount), 0) from SandboxWithdrawal w where w.walletId = :walletId "
            + "and w.fundsReleased = false and w.status in ('REQUESTED', 'PROCESSING')")
    BigDecimal reserved(@Param("walletId") UUID walletId);
}
