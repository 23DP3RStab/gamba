package com.gamba.backend.controller;

import com.gamba.backend.dto.ApiResponse;
import com.gamba.backend.dto.crash.CrashBetRequest;
import com.gamba.backend.dto.crash.CrashBetResponse;
import com.gamba.backend.dto.crash.CrashCashoutResponse;
import com.gamba.backend.dto.crash.CrashState;
import com.gamba.backend.model.User;
import com.gamba.backend.service.CrashEngine;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/games/crash")
@RequiredArgsConstructor
public class CrashController {

    private final CrashEngine crashEngine;

    // Live updates for the Crash page: events "state", "bet" and "cashout"
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return crashEngine.subscribe();
    }

    @GetMapping("/state")
    public ResponseEntity<ApiResponse<CrashState>> state() {
        return ResponseEntity.ok(ApiResponse.ok("Round", crashEngine.state()));
    }

    @PostMapping("/bet")
    public ResponseEntity<ApiResponse<CrashBetResponse>> bet(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CrashBetRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Bet placed",
                crashEngine.bet(user, request.getAmount(), request.getAutoCashout())));
    }

    @PostMapping("/cashout")
    public ResponseEntity<ApiResponse<CrashCashoutResponse>> cashout(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Cashed out", crashEngine.cashOut(user)));
    }
}
