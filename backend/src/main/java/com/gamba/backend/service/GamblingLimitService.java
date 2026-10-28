package com.gamba.backend.service;

import com.gamba.backend.dto.LimitsResponse;
import com.gamba.backend.model.User;
import com.gamba.backend.repository.CrashBetRepository;
import com.gamba.backend.repository.MinesGameRepository;
import com.gamba.backend.repository.PiggiesGameRepository;
import com.gamba.backend.repository.RouletteSpinRepository;
import com.gamba.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

// Responsible gambling: a daily loss limit and a self-lockout, checked before every bet.
// Lowering the limit is immediate; raising or removing it only applies after a 24-hour
// cooling-off period. A lockout can be extended but never shortened.
@Service
@RequiredArgsConstructor
public class GamblingLimitService {

    public static final Duration COOLING_OFF = Duration.ofHours(24);
    public static final Set<Integer> LOCKOUT_DAYS = Set.of(1, 7, 30, 180);

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZONE);

    private final UserRepository userRepository;
    private final MinesGameRepository minesGameRepository;
    private final RouletteSpinRepository rouletteSpinRepository;
    private final PiggiesGameRepository piggiesGameRepository;
    private final CrashBetRepository crashBetRepository;

    @Transactional
    public LimitsResponse getLimits(UUID userId) {
        User user = load(userId);
        return LimitsResponse.of(user, lostToday(userId));
    }

    @Transactional
    public LimitsResponse setLossLimit(UUID userId, BigDecimal limit) {
        User user = load(userId);
        changeLimit(user, limit, Instant.now());
        userRepository.save(user);
        return LimitsResponse.of(user, lostToday(userId));
    }

    @Transactional
    public LimitsResponse lockOut(UUID userId, int days) {
        User user = load(userId);
        lockOut(user, days, Instant.now());
        userRepository.save(user);
        return LimitsResponse.of(user, lostToday(userId));
    }

    // Called by every game before it takes a bet
    public void checkCanBet(User user, BigDecimal bet) {
        Instant now = Instant.now();
        if (applyPending(user, now)) {
            userRepository.save(user);
        }
        checkBet(user, bet, lostToday(user.getId()), now);
    }

    public boolean isLockedOut(User user) {
        return user.getLockedUntil() != null && Instant.now().isBefore(user.getLockedUntil());
    }

    public static String formatWhen(Instant instant) {
        return WHEN.format(instant);
    }

    // Net amount lost in all games since local midnight; a round still in progress counts in full
    public BigDecimal lostToday(UUID userId) {
        Instant since = LocalDate.now(ZONE).atStartOfDay(ZONE).toInstant();
        return minesGameRepository.lostSince(userId, since)
                .add(rouletteSpinRepository.lostSince(userId, since))
                .add(piggiesGameRepository.lostSince(userId, since))
                .add(crashBetRepository.lostSince(userId, since));
    }

    private User load(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (applyPending(user, Instant.now())) {
            userRepository.save(user);
        }
        return user;
    }

    // --- Rules, kept free of the database so they can be tested on their own ---

    // Applies a raised or removed limit once its cooling-off period is over
    static boolean applyPending(User user, Instant now) {
        if (user.getPendingLimitFrom() == null || now.isBefore(user.getPendingLimitFrom())) {
            return false;
        }
        user.setDailyLossLimit(user.getPendingLossLimit());
        user.setPendingLossLimit(null);
        user.setPendingLimitFrom(null);
        return true;
    }

    // `limit` null removes the limit
    static void changeLimit(User user, BigDecimal limit, Instant now) {
        applyPending(user, now);
        BigDecimal current = user.getDailyLossLimit();
        boolean stricter = limit != null && (current == null || limit.compareTo(current) <= 0);
        if (stricter || (limit == null && current == null)) {
            user.setDailyLossLimit(limit);
            user.setPendingLossLimit(null);
            user.setPendingLimitFrom(null);
        } else {
            user.setPendingLossLimit(limit);
            user.setPendingLimitFrom(now.plus(COOLING_OFF));
        }
    }

    static void lockOut(User user, int days, Instant now) {
        if (!LOCKOUT_DAYS.contains(days)) {
            throw new IllegalArgumentException("A lockout lasts 1, 7, 30 or 180 days");
        }
        Instant until = now.plus(Duration.ofDays(days));
        if (user.getLockedUntil() == null || until.isAfter(user.getLockedUntil())) {
            user.setLockedUntil(until);
        }
    }

    static void checkBet(User user, BigDecimal bet, BigDecimal lostToday, Instant now) {
        applyPending(user, now);
        if (user.getLockedUntil() != null && now.isBefore(user.getLockedUntil())) {
            throw new IllegalArgumentException("Your account is locked until " + formatWhen(user.getLockedUntil()));
        }
        BigDecimal limit = user.getDailyLossLimit();
        if (limit != null && lostToday.add(bet).compareTo(limit) > 0) {
            BigDecimal left = limit.subtract(lostToday).max(BigDecimal.ZERO).setScale(2);
            throw new IllegalArgumentException(
                    "This bet would go over your daily loss limit. You can lose €" + left + " more today");
        }
    }
}
