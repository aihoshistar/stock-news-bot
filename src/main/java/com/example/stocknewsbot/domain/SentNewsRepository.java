package com.example.stocknewsbot.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface SentNewsRepository extends JpaRepository<SentNews, Long> {

    boolean existsByLinkHash(String linkHash);

    @Modifying
    @Query("DELETE FROM SentNews WHERE sentAt < :cutoff")
    void deleteOlderThan(LocalDateTime cutoff);

    long countBySentAtAfter(LocalDateTime after);
}
