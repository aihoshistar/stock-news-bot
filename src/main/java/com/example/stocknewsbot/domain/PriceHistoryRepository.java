package com.example.stocknewsbot.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByQueriedAtAfterOrderByQueriedAtDesc(LocalDateTime after);
    long countByQueriedAtAfter(LocalDateTime after);
}
