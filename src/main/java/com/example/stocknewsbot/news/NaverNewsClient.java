package com.example.stocknewsbot.news;

import com.example.stocknewsbot.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class NaverNewsClient {
    private static final Logger log = LoggerFactory.getLogger(NaverNewsClient.class);
    private static final String BASE_URL = "https://openapi.naver.com";

    private final RestClient restClient;
    private final int newsDisplay;

    public NaverNewsClient(AppProperties appProperties) {
        this.newsDisplay = appProperties.naver().newsDisplay();
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("X-Naver-Client-Id",
                        appProperties.naver().clientId())
                .defaultHeader("X-Naver-Client-Secret",
                        appProperties.naver().clientSecret())
                .build();
    }

    /**
     * 종목명으로 뉴스 검색
     * @param stockName 종목 이름 (예: 에스엠)
     * @return 뉴스 정보 (title, link, pubDate)
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchNews(String stockName) {
        try {
            Map<String, Object> response = restClient.get().uri("/v1/search/news.json?query={query}&display={display}&sort=date", stockName, newsDisplay).retrieve().body(Map.class);

            if (response != null && response.containsKey("items")) {
                return (List<Map<String, Object>>) response.get("items");
            }
        } catch (Exception e) {
            log.error("네이버 뉴스 검색 실패 : stockName={}: {}", stockName, e.getMessage());
        }

        return List.of();
    }
}
