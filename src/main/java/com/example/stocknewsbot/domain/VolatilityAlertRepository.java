package com.example.stocknewsbot.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VolatilityAlertRepository extends JpaRepository<VolatilityAlert, Long> {
    List<VolatilityAlert> findByOccurredAtAfterOrderByOccurredAtDesc(LocalDateTime after);
}
