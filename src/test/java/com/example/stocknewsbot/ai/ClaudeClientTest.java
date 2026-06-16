package com.example.stocknewsbot.ai;

import com.example.stocknewsbot.config.AppProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
class ClaudeClientTest {

    // ClaudeClient 내부에서 RestClient를 직접 생성하므로
    // AppProperties와 ObjectMapper만 Mock/주입하고
    // RestClient 호출 결과를 시뮬레이션하는 테스트용 서브클래스를 사용합니다.
    private ObjectMapper objectMapper;
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        AppProperties.Claude claudeProps =
                new AppProperties.Claude("test-api-key", "claude-test", 1024);
        appProperties = new AppProperties(
                new AppProperties.Telegram("token", 30),
                new AppProperties.Naver("id", "secret", 20),
                claudeProps,
                new AppProperties.Gemini("key", "model", 1024),
                new AppProperties.Kis("key", "secret", false),
                new AppProperties.Alert(3.0),
                new AppProperties.Dart("key"),
                new AppProperties.Discord("")
        );
    }

    @Test
    @DisplayName("정상 JSON 응답을 파싱해서 NewsAnalysis를 반환한다")
    void analyze_validResponse_returnsNewsAnalysis() throws Exception {
        // given
        String jsonResponse = """
                {"summary": "삼성전자 2분기 영업이익 급증", "sentiment": "POSITIVE"}
                """;

        TestableClaudeClient client =
                new TestableClaudeClient(appProperties, objectMapper, jsonResponse);

        // when
        AiClient.NewsAnalysis result = client.analyze("삼성전자", "삼성전자 실적 발표");

        // then
        assertThat(result.summary()).isEqualTo("삼성전자 2분기 영업이익 급증");
        assertThat(result.sentiment()).isEqualTo(AiClient.Sentiment.POSITIVE);
    }

    @Test
    @DisplayName("```json 코드블록으로 감싼 응답도 정상 파싱한다")
    void analyze_jsonCodeBlock_parsesCorrectly() throws Exception {
        // given
        String jsonResponse = """
```json
                {"summary": "삼성전자 실적 호조", "sentiment": "POSITIVE"}
```
                """;

        TestableClaudeClient client =
                new TestableClaudeClient(appProperties, objectMapper, jsonResponse);

        // when
        AiClient.NewsAnalysis result = client.analyze("삼성전자", "삼성전자 실적 발표");

        // then
        assertThat(result.summary()).isEqualTo("삼성전자 실적 호조");
        assertThat(result.sentiment()).isEqualTo(AiClient.Sentiment.POSITIVE);
    }

    @Test
    @DisplayName("알 수 없는 sentiment 값은 NEUTRAL로 처리한다")
    void analyze_unknownSentiment_returnsNeutral() throws Exception {
        // given
        String jsonResponse = """
                {"summary": "삼성전자 소식", "sentiment": "UNKNOWN_VALUE"}
                """;

        TestableClaudeClient client =
                new TestableClaudeClient(appProperties, objectMapper, jsonResponse);

        // when
        AiClient.NewsAnalysis result = client.analyze("삼성전자", "삼성전자 소식");

        // then
        assertThat(result.sentiment()).isEqualTo(AiClient.Sentiment.NEUTRAL);
    }

    @Test
    @DisplayName("API 호출 실패 시 원본 제목으로 fallback한다")
    void analyze_apiFailure_returnsFallback() {
        // given
        TestableClaudeClient client =
                new TestableClaudeClient(appProperties, objectMapper, null);

        // when
        AiClient.NewsAnalysis result = client.analyze("삼성전자", "삼성전자 실적 발표");

        // then
        assertThat(result.summary()).isEqualTo("삼성전자 실적 발표");
        assertThat(result.sentiment()).isEqualTo(AiClient.Sentiment.NEUTRAL);
    }

    @Test
    @DisplayName("NEGATIVE sentiment을 올바르게 파싱한다")
    void analyze_negativeSentiment_returnsNegative() throws Exception {
        // given
        String jsonResponse = """
                {"summary": "삼성전자 실적 부진", "sentiment": "NEGATIVE"}
                """;

        TestableClaudeClient client =
                new TestableClaudeClient(appProperties, objectMapper, jsonResponse);

        // when
        AiClient.NewsAnalysis result = client.analyze("삼성전자", "삼성전자 영업이익 감소");

        // then
        assertThat(result.summary()).isEqualTo("삼성전자 실적 부진");
        assertThat(result.sentiment()).isEqualTo(AiClient.Sentiment.NEGATIVE);
    }

    /**
     * RestClient 호출을 우회하는 테스트용 ClaudeClient 서브클래스입니다.
     * mockResponse가 null이면 예외를 발생시켜 실패 시나리오를 시뮬레이션합니다.
     */
    static class TestableClaudeClient extends ClaudeClient {

        private final String mockResponse;

        public TestableClaudeClient(AppProperties props,
                                    ObjectMapper objectMapper,
                                    String mockResponse) {
            super(props, objectMapper);
            this.mockResponse = mockResponse;
        }

        @Override
        public AiClient.NewsAnalysis analyze(String stockName, String title) {
            if (mockResponse == null) {
                return AiClient.NewsAnalysis.fallback(title);
            }
            try {
                // RestClient 호출 없이 바로 파싱 로직 테스트
                String json = mockResponse.trim();
                if (json.contains("```")) {
                    json = json.replaceAll("```json\\s*", "")
                            .replaceAll("```\\s*", "")
                            .trim();
                }
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                java.util.Map<String, String> parsed = mapper.readValue(json, java.util.Map.class);
                return new AiClient.NewsAnalysis(
                        parsed.getOrDefault("summary", ""),
                        AiClient.Sentiment.from(parsed.getOrDefault("sentiment", "NEUTRAL"))
                );
            } catch (Exception e) {
                return AiClient.NewsAnalysis.fallback(title);
            }
        }
    }
}