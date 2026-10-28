package com.gamba.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.PLAYER;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    // Profile details, empty until the user fills them in on the profile page
    @Column(name = "first_name", length = 30)
    private String firstName;

    @Column(name = "last_name", length = 30)
    private String lastName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 8)
    private String phone;

    // Responsible gambling: a daily loss limit (raising or removing it waits 24 hours
    // in the pending fields) and a self-lockout that blocks betting and deposits
    @Column(name = "daily_loss_limit", precision = 15, scale = 2)
    private BigDecimal dailyLossLimit;

    @Column(name = "pending_loss_limit", precision = 15, scale = 2)
    private BigDecimal pendingLossLimit;

    @Column(name = "pending_limit_from")
    private Instant pendingLimitFrom;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Wallet wallet;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}