package com.gamba.backend.controller;

import com.gamba.backend.dto.ApiResponse;
import com.gamba.backend.dto.roulette.SpinRequest;
import com.gamba.backend.dto.roulette.SpinResponse;
import com.gamba.backend.model.User;
import com.gamba.backend.service.RouletteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games/roulette")
@RequiredArgsConstructor
public class RouletteController {

    private final RouletteService rouletteService;

    @PostMapping("/spin")
    public ResponseEntity<ApiResponse<SpinResponse>> spin(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SpinRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Spun", rouletteService.spin(user.getId(), request.getBets())));
    }
}
