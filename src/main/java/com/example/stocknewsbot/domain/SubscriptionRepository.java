package com.example.stocknewsbot.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // chat id 별로 전체 구독 목록 조히
    List<Subscription> findByChatId(Long chatId);

    // chat id 와 stock code 으로 특정 구독 조회
    Optional<Subscription> findByChatIdAndStockCode(Long chatId, String stockCode);

    // chat id 와 stock code 으로 삭제
    void deleteByChatIdAndStockCode(Long chatId, String stockCode);

    // 전체 구독에서 종목 코드 목록 조회
    List<Subscription> findDistinctByStockCodeIn(List<String> stockCodes);

    @Query("""
        SELECT s.stockCode, s.stockName, COUNT(DISTINCT s.chatId)
        FROM Subscription s
        GROUP BY s.stockCode, s.stockName
        ORDER BY COUNT(DISTINCT s.chatId) DESC
      """)
    List<Object[]> countSubscribersByStock();
}
