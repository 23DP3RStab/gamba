package com.gamba.backend.controller;

import com.gamba.backend.dto.ApiResponse;
import com.gamba.backend.dto.piggies.PiggiesGameResponse;
import com.gamba.backend.dto.piggies.StartPiggiesRequest;
import com.gamba.backend.model.User;
import com.gamba.backend.service.PiggiesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games/piggies")
@RequiredArgsConstructor
public class PiggiesController {

    private final PiggiesService piggiesService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<PiggiesGameResponse>> getActiveGame(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Active round", piggiesService.getActiveGame(user.getId())));
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<PiggiesGameResponse>> start(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody StartPiggiesRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Round started", piggiesService.start(user.getId(), request.getBetAmount())));
    }

    @PostMapping("/blow")
    public ResponseEntity<ApiResponse<PiggiesGameResponse>> blow(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Blown", piggiesService.blowNext(user.getId())));
    }

    @PostMapping("/cashout")
    public ResponseEntity<ApiResponse<PiggiesGameResponse>> cashout(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Cashed out", piggiesService.cashout(user.getId())));
    }
}
