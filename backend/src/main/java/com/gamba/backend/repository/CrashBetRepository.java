package com.gamba.backend.repository;

import com.gamba.backend.model.CrashBet;
import com.gamba.backend.model.CrashBetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface CrashBetRepository extends JpaRepository<CrashBet, UUID> {
    List<CrashBet> findByRoundIdAndStatus(UUID roundId, CrashBetStatus status);

    List<CrashBet> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Every bet of the round still in the air when it crashed is lost
    @Modifying(flushAutomatically = true)
    @Query("update CrashBet b set b.status = :lost where b.round.id = :roundId and b.status = :inProgress")
    int loseRemaining(@Param("roundId") UUID roundId,
                      @Param("inProgress") CrashBetStatus inProgress,
                      @Param("lost") CrashBetStatus lost);

    // Net amount lost since `since`; a bet still in the air counts in full, a refund as nothing
    @Query("select coalesce(sum(b.betAmount - b.payout), 0) from CrashBet b "
            + "where b.user.id = :userId and b.createdAt >= :since")
    BigDecimal lostSince(@Param("userId") UUID userId, @Param("since") Instant since);
}
