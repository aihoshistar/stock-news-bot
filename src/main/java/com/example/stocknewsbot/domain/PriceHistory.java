package com.example.stocknewsbot.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "price_history")
public class PriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "stock_name", nullable = false, length = 50)
    private String stockName;

    @Column(name = "current_price", nullable = false)
    private long currentPrice;

    @Column(name = "change_rate", nullable = false)
    private double changeRate;

    @Column(name = "queried_at", nullable = false)
    private LocalDateTime queriedAt;

    protected PriceHistory() {}

    public PriceHistory(String stockCode, String stockName, long currentPrice, double changeRate) {
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.currentPrice = currentPrice;
        this.changeRate = changeRate;
        this.queriedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getStockCode() { return stockCode; }
    public String getStockName() { return stockName; }
    public long getCurrentPrice() { return currentPrice; }
    public double getChangeRate() { return changeRate; }
    public LocalDateTime getQueriedAt() { return queriedAt; }
}
