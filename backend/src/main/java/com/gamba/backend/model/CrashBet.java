package com.gamba.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crash_bets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrashBet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private CrashRound round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bet_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal betAmount;

    // Cash out by itself at this multiplier; null when the player cashes out by hand
    @Column(name = "auto_cashout", precision = 10, scale = 2)
    private BigDecimal autoCashout;

    @Column(name = "cashout_multiplier", precision = 10, scale = 2)
    private BigDecimal cashoutMultiplier;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal payout = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CrashBetStatus status = CrashBetStatus.IN_PROGRESS;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
