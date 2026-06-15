package com.example.stocknewsbot.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "subscription", uniqueConstraints = @UniqueConstraint(columnNames = {"chat_id", "stock_code"}))
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "stock_name", nullable = false, length = 50)
    private String stockName;

    protected Subscription() {}

    public Subscription(Long chatId, String stockCode, String stockName) {
        this.chatId = chatId;
        this.stockCode = stockCode;
        this.stockName = stockName;
    }

    public Long getId() { return id; }
    public Long getChatId() { return chatId; }
    public String getStockCode() { return stockCode; }
    public String getStockName() { return stockName; }
}
