package com.example.stocknewsbot.web;

import java.time.LocalDateTime;

public record TimelineItem(
        LocalDateTime occurredAt,
        TimelineType type,
        String stockCode,
        String stockName,
        String title,
        String detail,
        String link
) {
    public enum TimelineType {
        NEWS("뉴스/공시", "📰"),
        PRICE("시세", "📊"),
        VOLATILITY("급변동", "🚨");

        private final String label;
        private final String icon;

        TimelineType(String label, String icon) {
            this.label = label;
            this.icon = icon;
        }

        public String label() { return label; }
        public String icon() { return icon; }
    }
}
