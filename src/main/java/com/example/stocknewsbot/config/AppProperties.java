package com.example.stocknewsbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Telegram telegram, Naver naver) {
    public record Telegram(String token, long pollingTimeout) {}

    public record Naver(String clientId, String clientSecret, int newsDisplay) {}
}
