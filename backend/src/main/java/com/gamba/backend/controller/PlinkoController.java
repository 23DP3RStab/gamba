package com.gamba.backend.controller;

import com.gamba.backend.dto.ApiResponse;
import com.gamba.backend.dto.plinko.PlinkoDropRequest;
import com.gamba.backend.dto.plinko.PlinkoDropResponse;
import com.gamba.backend.model.PlinkoRisk;
import com.gamba.backend.model.User;
import com.gamba.backend.service.PlinkoRules;
import com.gamba.backend.service.PlinkoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/games/plinko")
@RequiredArgsConstructor
public class PlinkoController {

    private final PlinkoService plinkoService;

    // The payout per slot for each risk level, so the board shows exactly what the server pays
    @GetMapping("/tables")
    public ResponseEntity<ApiResponse<Map<PlinkoRisk, List<BigDecimal>>>> tables() {
        return ResponseEntity.ok(ApiResponse.ok("Tables", PlinkoRules.MULTIPLIERS));
    }

    @PostMapping("/drop")
    public ResponseEntity<ApiResponse<PlinkoDropResponse>> drop(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PlinkoDropRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Dropped",
                plinkoService.drop(user.getId(), request.getBetAmount(), request.getRisk())));
    }
}
