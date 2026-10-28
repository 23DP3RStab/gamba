package com.gamba.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "piggies_games")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PiggiesGame {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bet_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal betAmount;

    // How many of the three houses the wolf has blown down so far
    @Column(name = "houses_down", nullable = false)
    @Builder.Default
    private int housesDown = 0;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal payout = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PiggiesGameStatus status = PiggiesGameStatus.IN_PROGRESS;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
