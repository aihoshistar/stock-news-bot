package com.example.stocknewsbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Telegram telegram) {
    public record Telegram(String token, long pollingTimeout) {}
}
