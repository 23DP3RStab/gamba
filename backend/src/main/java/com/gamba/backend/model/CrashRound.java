package com.gamba.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crash_rounds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrashRound {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Drawn when the round is created, kept from the players until the crash
    @Column(name = "crash_point", nullable = false, precision = 10, scale = 2)
    private BigDecimal crashPoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CrashRoundStatus status = CrashRoundStatus.BETTING;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "crashed_at")
    private Instant crashedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
