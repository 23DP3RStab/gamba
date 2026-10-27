package com.gamba.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mines_games")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinesGame {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bet_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal betAmount;

    @Column(name = "mine_count", nullable = false)
    private int mineCount;

    @Column(name = "mine_positions", nullable = false, length = 150)
    private String minePositions;

    @Column(name = "revealed_tiles", nullable = false, length = 150)
    @Builder.Default
    private String revealedTiles = "";

    @Column(name = "current_multiplier", nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal currentMultiplier = BigDecimal.ONE;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal payout = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MinesGameStatus status = MinesGameStatus.IN_PROGRESS;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}