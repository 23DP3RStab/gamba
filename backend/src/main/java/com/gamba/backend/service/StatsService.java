package com.gamba.backend.service;

import com.gamba.backend.dto.GameStats;
import com.gamba.backend.dto.StatsResponse;
import com.gamba.backend.model.MinesGameStatus;
import com.gamba.backend.model.PiggiesGameStatus;
import com.gamba.backend.repository.MinesGameRepository;
import com.gamba.backend.repository.PiggiesGameRepository;
import com.gamba.backend.repository.RouletteSpinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// Per-game stats for the profile page, over the player's finished rounds
@Service
@RequiredArgsConstructor
public class StatsService {

    private final MinesGameRepository minesGameRepository;
    private final RouletteSpinRepository rouletteSpinRepository;
    private final PiggiesGameRepository piggiesGameRepository;

    @Transactional(readOnly = true)
    public StatsResponse getStats(UUID userId) {
        return StatsResponse.builder()
                .mines(GameStats.of(minesGameRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                        .filter(g -> g.getStatus() != MinesGameStatus.IN_PROGRESS)
                        .map(g -> new GameStats.Round(g.getBetAmount(), g.getPayout()))
                        .toList()))
                .roulette(GameStats.of(rouletteSpinRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                        .map(s -> new GameStats.Round(s.getTotalBet(), s.getPayout()))
                        .toList()))
                .piggies(GameStats.of(piggiesGameRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                        .filter(g -> g.getStatus() != PiggiesGameStatus.IN_PROGRESS)
                        .map(g -> new GameStats.Round(g.getBetAmount(), g.getPayout()))
                        .toList()))
                .build();
    }
}
