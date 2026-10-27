package com.gamba.backend.wallet;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SandboxWalletRepository extends JpaRepository<SandboxWallet, UUID> {
    Optional<SandboxWallet> findByAccessKeyHash(String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from SandboxWallet w where w.id = :id")
    Optional<SandboxWallet> lockById(@Param("id") UUID id);
}
