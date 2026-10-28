package com.gamba.backend.repository;

import com.gamba.backend.model.PlinkoDrop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface PlinkoDropRepository extends JpaRepository<PlinkoDrop, UUID> {
    List<PlinkoDrop> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Net amount lost on drops since `since` (negative when the player is ahead)
    @Query("select coalesce(sum(d.betAmount - d.payout), 0) from PlinkoDrop d "
            + "where d.user.id = :userId and d.createdAt >= :since")
    BigDecimal lostSince(@Param("userId") UUID userId, @Param("since") Instant since);
}
