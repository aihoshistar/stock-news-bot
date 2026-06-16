package com.example.stocknewsbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Telegram telegram, Naver naver, Claude claude, Gemini gemini) {
    public record Telegram(String token, long pollingTimeout) {}

    public record Naver(String clientId, String clientSecret, int newsDisplay) {}

    public record Claude(String apiKey, String model, int maxToken ) {}

    public record Gemini(String apiKey, String model, int maxTokens ) {}
}
