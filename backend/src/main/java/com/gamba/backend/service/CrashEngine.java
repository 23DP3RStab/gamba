package com.gamba.backend.service;

import com.gamba.backend.dto.crash.CrashBetResponse;
import com.gamba.backend.dto.crash.CrashCashoutResponse;
import com.gamba.backend.dto.crash.CrashState;
import com.gamba.backend.model.User;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

// The live Crash round that everyone plays together. It lives in memory and runs
// betting (8 s) → flying → crashed (4 s, showing where) → the next round. A tick every 100 ms
// moves it along and pays auto cash-outs at exactly their target. Every change is pushed to the
// open pages as Server-Sent Events; the money always goes through CrashService first.
// All state is guarded by this object's lock.
@Component
@RequiredArgsConstructor
public class CrashEngine {

    public enum Phase { BETTING, RUNNING, CRASHED }

    private static final int HISTORY_KEPT = 20;
    private static final long HEARTBEAT_MS = 15_000;
    // The page reconnects when a stream ends, so it doesn't need to stay open forever
    private static final long STREAM_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final CrashService crashService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // The current round; roundId stays null until the app is ready
    private UUID roundId;
    private Phase phase;
    private BigDecimal crashPoint;
    private long crashAfterMs;
    private long bettingEndsAt;
    private long startedAt;
    private long nextRoundAt;
    private final Map<UUID, LiveBet> bets = new LinkedHashMap<>(); // by player
    private final Deque<BigDecimal> history = new ArrayDeque<>(); // newest first
    private long lastHeartbeat;

    private static final class LiveBet {
        final UUID betId;
        final String username;
        final BigDecimal amount;
        final BigDecimal autoCashout;
        BigDecimal cashedOutAt;
        BigDecimal payout;

        LiveBet(UUID betId, String username, BigDecimal amount, BigDecimal autoCashout) {
            this.betId = betId;
            this.username = username;
            this.amount = amount;
            this.autoCashout = autoCashout;
        }

        CrashState.PlayerBet view() {
            return new CrashState.PlayerBet(username, amount, cashedOutAt, payout);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void start() {
        crashService.refundUnfinished();
        history.addAll(crashService.history());
        newRound(System.currentTimeMillis());
    }

    @Scheduled(fixedDelay = 100)
    public synchronized void tick() {
        if (roundId == null) return;
        long now = System.currentTimeMillis();
        advance(now);
        if (now - lastHeartbeat >= HEARTBEAT_MS) {
            lastHeartbeat = now;
            for (SseEmitter emitter : emitters) {
                send(emitter, SseEmitter.event().comment("ping"));
            }
        }
    }

    // Opens a live stream for one page: the round as it is now, then every change
    public synchronized SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        if (send(emitter, event("state", state()))) {
            emitters.add(emitter);
        }
        return emitter;
    }

    public synchronized CrashState state() {
        boolean flying = phase == Phase.RUNNING || phase == Phase.CRASHED;
        return CrashState.builder()
                .roundId(roundId)
                .phase(phase == null ? null : phase.name())
                .serverNow(System.currentTimeMillis())
                .bettingEndsAt(bettingEndsAt)
                .startedAt(flying ? startedAt : null)
                .crashPoint(phase == Phase.CRASHED ? crashPoint : null)
                .bets(bets.values().stream().map(LiveBet::view).toList())
                .history(List.copyOf(history))
                .build();
    }

    public synchronized CrashBetResponse bet(User user, BigDecimal amount, BigDecimal autoCashout) {
        advance(System.currentTimeMillis());
        CrashRules.checkBet(amount, autoCashout);
        if (phase != Phase.BETTING) {
            throw new IllegalArgumentException("Bets are closed, wait for the next round");
        }
        if (bets.containsKey(user.getId())) {
            throw new IllegalArgumentException("You already have a bet on this round");
        }

        CrashService.PlacedBet placed = crashService.placeBet(user.getId(), roundId, amount, autoCashout);
        LiveBet bet = new LiveBet(placed.betId(), placed.username(), amount, autoCashout);
        bets.put(user.getId(), bet);
        broadcast("bet", bet.view());
        return CrashBetResponse.builder()
                .amount(amount)
                .autoCashout(autoCashout)
                .balance(placed.balance())
                .build();
    }

    // Cashes out at the multiplier the round is at when the request reaches the server
    public synchronized CrashCashoutResponse cashOut(User user) {
        long now = System.currentTimeMillis();
        advance(now);
        LiveBet bet = bets.get(user.getId());
        if (bet == null) {
            throw new IllegalArgumentException("You have no bet on this round");
        }
        if (bet.cashedOutAt != null) {
            throw new IllegalArgumentException("Already cashed out at " + bet.cashedOutAt + "×");
        }
        if (phase == Phase.BETTING) {
            throw new IllegalArgumentException("The round hasn't started yet");
        }
        if (phase == Phase.CRASHED) {
            throw new IllegalArgumentException("Too late, it crashed at " + crashPoint + "×");
        }

        BigDecimal multiplier = CrashRules.multiplierAt(now - startedAt);
        CrashService.CashedOut result = pay(bet, multiplier);
        return CrashCashoutResponse.builder()
                .multiplier(multiplier)
                .payout(result.payout())
                .balance(result.balance())
                .build();
    }

    @PreDestroy
    public void close() {
        for (SseEmitter emitter : emitters) {
            emitter.complete();
        }
    }

    // Moves the round along to where it should be at `now`
    private void advance(long now) {
        if (phase == Phase.BETTING && now >= bettingEndsAt) {
            crashService.startRound(roundId, Instant.ofEpochMilli(now));
            phase = Phase.RUNNING;
            startedAt = now;
            broadcast("state", state());
        }
        if (phase == Phase.RUNNING) {
            long flown = now - startedAt;
            for (LiveBet bet : bets.values()) {
                if (bet.cashedOutAt == null && bet.autoCashout != null
                        && bet.autoCashout.compareTo(crashPoint) <= 0
                        && CrashRules.msUntil(bet.autoCashout) <= flown) {
                    pay(bet, bet.autoCashout);
                }
            }
            if (flown >= crashAfterMs) {
                crashService.crash(roundId, Instant.ofEpochMilli(now));
                phase = Phase.CRASHED;
                nextRoundAt = now + CrashRules.RESULT_MS;
                history.addFirst(crashPoint);
                while (history.size() > HISTORY_KEPT) history.removeLast();
                broadcast("state", state());
            }
        }
        if (phase == Phase.CRASHED && now >= nextRoundAt) {
            newRound(now);
        }
    }

    private void newRound(long now) {
        BigDecimal point = CrashRules.crashPoint(secureRandom.nextDouble());
        roundId = crashService.newRound(point);
        crashPoint = point;
        crashAfterMs = CrashRules.msUntil(point);
        phase = Phase.BETTING;
        bettingEndsAt = now + CrashRules.BETTING_MS;
        bets.clear();
        broadcast("state", state());
    }

    private CrashService.CashedOut pay(LiveBet bet, BigDecimal multiplier) {
        CrashService.CashedOut result = crashService.cashOut(bet.betId, multiplier);
        bet.cashedOutAt = multiplier;
        bet.payout = result.payout();
        broadcast("cashout", bet.view());
        return result;
    }

    private static SseEmitter.SseEventBuilder event(String name, Object data) {
        return SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON);
    }

    private void broadcast(String name, Object data) {
        for (SseEmitter emitter : emitters) {
            send(emitter, event(name, data));
        }
    }

    // A page that went away is dropped from the list
    private boolean send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
            return true;
        } catch (Exception e) {
            emitters.remove(emitter);
            return false;
        }
    }
}
