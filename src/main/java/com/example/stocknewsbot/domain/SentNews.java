package com.example.stocknewsbot.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "sent_news", uniqueConstraints = @UniqueConstraint(columnNames = {"link_hash"})
)
public class SentNews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "link_hash", nullable = false, length = 64)
    private String linkHash;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    protected  SentNews() {}

    public SentNews(String linkHash, String stockCode) {
        this.linkHash = linkHash;
        this.stockCode = stockCode;
        this.sentAt = LocalDateTime.now();
    }

    public String getLinkHash() { return linkHash; }
    public String getStockCode() { return stockCode; }
    public LocalDateTime getSentAt() { return sentAt; }
}
