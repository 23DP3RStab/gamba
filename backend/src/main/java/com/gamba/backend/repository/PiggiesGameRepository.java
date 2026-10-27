package com.gamba.backend.repository;

import com.gamba.backend.model.PiggiesGame;
import com.gamba.backend.model.PiggiesGameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PiggiesGameRepository extends JpaRepository<PiggiesGame, UUID> {
    Optional<PiggiesGame> findByUserIdAndStatus(UUID userId, PiggiesGameStatus status);

    List<PiggiesGame> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Net amount lost since `since`; a round still in progress counts as its whole bet
    @Query("select coalesce(sum(g.betAmount - g.payout), 0) from PiggiesGame g "
            + "where g.user.id = :userId and g.createdAt >= :since")
    BigDecimal lostSince(@Param("userId") UUID userId, @Param("since") Instant since);
}
