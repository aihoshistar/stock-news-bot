package com.example.stocknewsbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Telegram telegram, Naver naver, Claude claude, Gemini gemini, Kis kis, Alert alert, Dart dart, Discord discord) {
    public record Telegram(String token, long pollingTimeout) {}

    public record Naver(String clientId, String clientSecret, int newsDisplay) {}

    public record Claude(String apiKey, String model, int maxTokens ) {}

    public record Gemini(String apiKey, String model, int maxTokens ) {}

    public record Kis(String appKey, String appSecret, boolean virtual) {}

    public record Alert(double volatilityThreshold) {}

    public record Dart(String apiKey) {}

    public record Discord(String webhookUrl) {}
}
