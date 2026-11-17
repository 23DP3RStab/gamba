package com.gamba.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plinko_drops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlinkoDrop {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bet_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal betAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PlinkoRisk risk;

    // The 12 bounces as L and R, top to bottom
    @Column(nullable = false, length = 20)
    private String path;

    // Where the ball landed: 0 (far left) to 12 (far right)
    @Column(nullable = false)
    private int slot;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal multiplier;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal payout = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
