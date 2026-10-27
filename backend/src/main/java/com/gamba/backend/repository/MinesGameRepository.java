package com.gamba.backend.repository;

import com.gamba.backend.model.MinesGame;
import com.gamba.backend.model.MinesGameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MinesGameRepository extends JpaRepository<MinesGame, UUID> {
    Optional<MinesGame> findByUserIdAndStatus(UUID userId, MinesGameStatus status);
}