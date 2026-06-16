package com.example.stocknewsbot.ai;

import com.example.stocknewsbot.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;


import java.util.List;
import java.util.Map;

@Component
public class ClaudeClient {
    private static final Logger log = LoggerFactory.getLogger(ClaudeClient.class);
    private static final String BASE_URL = "https://api.anthropic.com";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final String model;
    private final int maxTokens;
    private final ObjectMapper objectMapper;

    public ClaudeClient(AppProperties appProperties, ObjectMapper objectMapper) {
        this.model = appProperties.claude().model();
        this.maxTokens = appProperties.claude().maxToken();
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(BASE_URL)
                .defaultHeader("x-api-key", appProperties.claude().apiKey())
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     *
     * @param stockName 주식 이름
     * @param title     뉴스 제목
     * @return          분석 결과 (요약, 시장 심리)
     */
    public NewsAnalysis analyze(String stockName, String title) {
        String prompt = buildPrompt(stockName, title);

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "max_tokens", maxTokens,
                    "message", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            Map<String, Object> response = restClient.post().uri("/v1/messages").body(requestBody).retrieve().body(Map.class);

            return parseResponse(response);
        } catch (Exception e) {
            log.error("Claude 분석 실패 stockName={} title={}: {}", stockName, title, e.getMessage());
            return NewsAnalysis.fallback(title);
        }
    }

    @SuppressWarnings("unchecked")
    private NewsAnalysis parseResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> content =
                    (List<Map<String, Object>>) response.get("content");
            String text = (String) content.get(0).get("text");

            // JSON 블록 추출
            String json = text.trim();
            if (json.contains("```")) {
                json = json.replaceAll("```json\\s*", "")
                        .replaceAll("```\\s*", "")
                        .trim();
            }

            Map<String, String> parsed = objectMapper.readValue(json, Map.class);
            return new NewsAnalysis(
                    parsed.getOrDefault("summary", ""),
                    Sentiment.from(parsed.getOrDefault("sentiment", "NEUTRAL"))
            );
        } catch (Exception e) {
            log.error("Claude 응답 파싱 실패: {}", e.getMessage());
            return NewsAnalysis.fallback("");
        }
    }

    private String buildPrompt(String stockName, String title) {
        return """
                다음 뉴스 제목을 분석해줘.
                
                종목명: %s
                뉴스 제목: %s
                
                아래 JSON 형식으로만 응답해. 다른 설명은 하지 마.
                {
                  "summary": "뉴스를 한 문장으로 요약 (50자 이내)",
                  "sentiment": "POSITIVE 또는 NEUTRAL 또는 NEGATIVE 중 하나"
                }
                """.formatted(stockName, title);
    }

    public record NewsAnalysis(String summary, Sentiment sentiment) {
        public static NewsAnalysis fallback(String title) {
            return new NewsAnalysis(title, Sentiment.NEUTRAL);
        }
    }

    public enum Sentiment {
        POSITIVE, NEUTRAL, NEGATIVE;

        public static Sentiment from(String value) {
            try {
                return Sentiment.valueOf(value.toUpperCase());
            } catch (Exception e) {
                return NEUTRAL;
            }
        }

        public String emoji() {
            return switch (this) {
                case POSITIVE -> "📈";
                case NEGATIVE -> "📉";
                case NEUTRAL  -> "➡️";
            };
        }
    }
}