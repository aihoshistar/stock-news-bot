package com.example.stocknewsbot.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class FallbackAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(FallbackAiClient.class);

    private final GeminiClient geminiClient;
    private final ClaudeClient claudeClient;

    public FallbackAiClient(GeminiClient geminiClient, ClaudeClient claudeClient) {
        this.geminiClient = geminiClient;
        this.claudeClient = claudeClient;
    }

    public NewsAnalysis analyze(String stockName, String title) {
        try {
            NewsAnalysis result = geminiClient.analyze(stockName, title);
            if (isValid(result)) {
                log.debug("Gemini 분석 성공 stockName={}", stockName);
                return result;
            }
            log.warn("Gemini 결과 유효하지 않음 stockName={}, Claude로 fallback", stockName);
        } catch (Exception e) {
            log.warn("Gemini 호출 실패 stockName={}: {}, Claude로 fallback", stockName, e.getMessage());
        }

        try {
            NewsAnalysis result = claudeClient.analyze(stockName, title);
            if (isValid(result)) {
                log.debug("Claude fallback 성공 stockName={}", stockName);
                return result;
            }
        } catch (Exception e) {
            log.error("Claude fallback 실패 stockName={}: {}", stockName, e.getMessage());
        }

        log.error("모든 AI 실패 StockName={}, 원본 제목으로 대체, title={}", stockName, title);
        return NewsAnalysis.fallback(title);
    }

    //
    public boolean isValid(NewsAnalysis result) {
        return result != null && result.summary() != null && !result.summary().isBlank();
    }
}
