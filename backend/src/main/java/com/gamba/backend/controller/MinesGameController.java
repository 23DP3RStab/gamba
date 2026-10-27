package com.gamba.backend.controller;

import com.gamba.backend.dto.ApiResponse;
import com.gamba.backend.dto.mines.MinesGameResponse;
import com.gamba.backend.dto.mines.RevealTileRequest;
import com.gamba.backend.dto.mines.StartMinesRequest;
import com.gamba.backend.model.User;
import com.gamba.backend.service.MinesGameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games/mines")
@RequiredArgsConstructor
public class MinesGameController {

    private final MinesGameService minesGameService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<MinesGameResponse>> getActiveGame(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Active game", minesGameService.getActiveGame(user.getId())));
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<MinesGameResponse>> startGame(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody StartMinesRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Game started", minesGameService.startGame(user.getId(), request)));
    }

    @PostMapping("/reveal")
    public ResponseEntity<ApiResponse<MinesGameResponse>> revealTile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RevealTileRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Tile revealed", minesGameService.revealTile(user.getId(), request.getTileIndex())));
    }

    @PostMapping("/cashout")
    public ResponseEntity<ApiResponse<MinesGameResponse>> cashout(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok("Cashed out", minesGameService.cashout(user.getId())));
    }
}