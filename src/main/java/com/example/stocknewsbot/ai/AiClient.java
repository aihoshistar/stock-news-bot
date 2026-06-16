package com.example.stocknewsbot.ai;

public interface AiClient {

    /**
     * 뉴스 제목을 요약하고 주가 영향도를 분석합니다.
     *
     * @param stockName 종목명
     * @param title     뉴스 제목
     * @return 분석 결과
     */
    NewsAnalysis analyze(String stockName, String title);

    record NewsAnalysis(String summary, Sentiment sentiment) {
        public static NewsAnalysis fallback(String title) {
            return new NewsAnalysis(title, Sentiment.NEUTRAL);
        }
    }

    enum Sentiment {
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
    }}
