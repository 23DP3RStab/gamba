package com.gamba.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "roulette_spins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouletteSpin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_bet", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalBet;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal payout = BigDecimal.ZERO;

    @Column(name = "winning_number", nullable = false)
    private int winningNumber;

    // The placed bets as "key:amount,key:amount"
    @Column(nullable = false, length = 8000)
    private String bets;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
