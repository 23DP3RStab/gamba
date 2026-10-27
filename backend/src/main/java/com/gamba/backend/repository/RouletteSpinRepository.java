package com.gamba.backend.repository;

import com.gamba.backend.model.RouletteSpin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface RouletteSpinRepository extends JpaRepository<RouletteSpin, UUID> {
    List<RouletteSpin> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Net amount lost on spins since `since` (negative when the player is ahead)
    @Query("select coalesce(sum(s.totalBet - s.payout), 0) from RouletteSpin s "
            + "where s.user.id = :userId and s.createdAt >= :since")
    BigDecimal lostSince(@Param("userId") UUID userId, @Param("since") Instant since);
}
