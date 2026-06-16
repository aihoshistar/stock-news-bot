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
public class GeminiClient implements AiClient {
    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final String BASE_URL = "https://generativelanguage.googleapis.com";

    private final RestClient restClient;
    private final String model;
    private final int maxTokens;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public GeminiClient(AppProperties appProperties, ObjectMapper objectMapper) {
        this.model = appProperties.gemini().model();
        this.maxTokens = appProperties.gemini().maxTokens();
        this.apiKey = appProperties.gemini().apiKey();
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(BASE_URL).defaultHeader("Content-Type", "application/json").build();
    }

    @Override
    public NewsAnalysis analyze(String stockName, String title) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", buildPrompt(stockName, title))
                            ))
                    ),
                    "generationConfig", Map.of(
                            "maxOutputTokens", maxTokens,
                            "temperature", 0.1
                    )
            );

            Map<String, Object> response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            return parseResponse(response, title);

        } catch (Exception e) {
            log.error("Gemini 분석 실패 stockName={}: {}", stockName, e.getMessage());
            return NewsAnalysis.fallback(title);
        }
    }

    @SuppressWarnings("unchecked")
    private NewsAnalysis parseResponse(Map<String, Object> response, String title) {
        try {
            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content =
                    (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts =
                    (List<Map<String, Object>>) content.get("parts");
            String text = (String) parts.get(0).get("text");

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
            log.error("Gemini 응답 파싱 실패: {}", e.getMessage());
            return NewsAnalysis.fallback(title);
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
}
