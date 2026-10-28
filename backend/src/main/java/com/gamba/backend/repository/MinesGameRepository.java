package com.gamba.backend.repository;

import com.gamba.backend.model.MinesGame;
import com.gamba.backend.model.MinesGameStatus;
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
public interface MinesGameRepository extends JpaRepository<MinesGame, UUID> {
    Optional<MinesGame> findByUserIdAndStatus(UUID userId, MinesGameStatus status);

    List<MinesGame> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Net amount lost since `since`; a game still in progress counts as its whole bet
    @Query("select coalesce(sum(g.betAmount - g.payout), 0) from MinesGame g "
            + "where g.user.id = :userId and g.createdAt >= :since")
    BigDecimal lostSince(@Param("userId") UUID userId, @Param("since") Instant since);
}
