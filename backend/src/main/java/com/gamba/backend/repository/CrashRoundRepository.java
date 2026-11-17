package com.gamba.backend.repository;

import com.gamba.backend.model.CrashRound;
import com.gamba.backend.model.CrashRoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface CrashRoundRepository extends JpaRepository<CrashRound, UUID> {
    List<CrashRound> findByStatusIn(Collection<CrashRoundStatus> statuses);

    List<CrashRound> findTop20ByStatusOrderByCreatedAtDesc(CrashRoundStatus status);
}
