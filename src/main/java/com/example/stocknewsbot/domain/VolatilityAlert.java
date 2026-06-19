package com.example.stocknewsbot.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "volatility_alert")
public class VolatilityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "stock_name", nullable = false, length = 50)
    private String stockName;

    @Column(name = "base_price", nullable = false)
    private long basePrice;

    @Column(name = "current_price", nullable = false)
    private long currentPrice;

    @Column(name = "change_rate", nullable = false)
    private double changeRate;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    protected VolatilityAlert() {}

    public VolatilityAlert(String stockCode, String stockName, long basePrice, long currentPrice, double changeRate) {
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.basePrice = basePrice;
        this.currentPrice = currentPrice;
        this.changeRate = changeRate;
        this.occurredAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getStockCode() { return stockCode; }
    public String getStockName() { return stockName; }
    public long getBasePrice() { return  currentPrice; }
    public long getCurrentPrice() { return currentPrice; }
    public double getChangeRate() { return  changeRate; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
