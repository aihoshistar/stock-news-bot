package com.example.stocknewsbot.price;

import com.example.stocknewsbot.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class KisTokenManager {

    private static final Logger log = LoggerFactory.getLogger(KisTokenManager.class);

    private final RestClient restClient;
    private final String appKey;
    private final String appSecret;

    private String cachedToken;
    private LocalDateTime tokenExpiry;


    public  KisTokenManager(AppProperties appProperties) {
        AppProperties.Kis kis = appProperties.kis();
        this.appKey = kis.appKey();
        this.appSecret = kis.appSecret();

        String baseUrl = kis.virtual()
                ? "https://openapivts.koreainvestment.com:9443"
                : "https://openapi.koreainvestment.com:9443";

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    // 여러 쓰레드에서 중복 생성되지 않도록 처리
    public synchronized String getAccessToken() {
        if (cachedToken != null && LocalDateTime.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }

        return issueToken();
    }

    @SuppressWarnings("unchecked")
    private String issueToken() {
        log.info("KIS Access Token 발급 요청");
        try {
            Map<String, Object> response = restClient.post().uri("/oauth2/tokenP").body(Map.of(
                    "grant_type", "client_credentials",
                    "appkey", appKey,
                    "appsecret", appSecret
            )).retrieve().body(Map.class);

            cachedToken = (String) response.get("access_token");
            tokenExpiry = LocalDateTime.now().plusHours(23); // 1시간 여유있게
            log.info("KIS Access token 발급 요청. 만료: {}", tokenExpiry);
            return cachedToken;
        } catch (Exception e) {
            log.error("KIS 도큰 발급 실패: {}", e.getMessage());;
            throw new IllegalStateException("KIS 토큰 발급 실패", e);
        }
    }
}
