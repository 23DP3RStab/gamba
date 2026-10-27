package com.gamba.backend.wallet;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SandboxDepositRepository extends JpaRepository<SandboxDeposit, UUID> {
    Optional<SandboxDeposit> findByPaypalOrderIdAndWalletId(String orderId, UUID walletId);
}
