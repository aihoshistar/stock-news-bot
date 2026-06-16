package com.example.stocknewsbot.price;

import com.example.stocknewsbot.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class KisClient {
    private static final Logger log = LoggerFactory.getLogger(KisClient.class);

    private final RestClient restClient;
    private final KisTokenManager tokenManager;
    private final String appKey;
    private final String appSecret;
    private final boolean virtual;

    public KisClient(AppProperties appProperties, KisTokenManager tokenManager) {
        AppProperties.Kis kis = appProperties.kis();
        this.tokenManager = tokenManager;
        this.appKey = kis.appKey();
        this.appSecret = kis.appSecret();
        this.virtual = kis.virtual();

        String baseUrl = kis.virtual()
                ? "https://openapivts.koreainvestment.com:9443"
                : "https://openapi.koreainvestment.com:9443";

        this.restClient = RestClient.builder().baseUrl(baseUrl).defaultHeader("Content-Type", "application/json").build();
    }

    /**
     * 종목 코드로 현재 가격 조회
     * @param stockCode 종목 코드
     * @return          시세 정보
     */
    @SuppressWarnings("unchecked")
    public PriceInfo getCurrentPrice(String stockCode) {
        try {
            String trId = virtual ? "VTTC8434R" : "FHKST01010100";

            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", stockCode)
                            .build())
                    .header("authorization", "Bearer " + tokenManager.getAccessToken())
                    .header("appkey", appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id", trId)
                    .retrieve()
                    .body(Map.class);

            return parsePriceInfo(stockCode, response);
        } catch (Exception e) {
            log.error("KIS 실시간 가격 조회 실패 stockCode={}: {}", stockCode, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private PriceInfo parsePriceInfo(String stockCode, Map<String, Object> response) {
        try {
            Map<String, Object> output = (Map<String, Object>) response.get("output");
            String currentPrice = (String) output.get("stck_prpr");   // 현재가
            String changePrice  = (String) output.get("prdy_vrss");   // 전일 대비
            String changeRate   = (String) output.get("prdy_ctrt");   // 등락률
            String highPrice    = (String) output.get("stck_hgpr");   // 고가
            String lowPrice     = (String) output.get("stck_lwpr");   // 저가
            String volume       = (String) output.get("acml_vol");    // 거래량

            return new PriceInfo(
                    stockCode,
                    Long.parseLong(currentPrice),
                    Long.parseLong(changePrice),
                    Double.parseDouble(changeRate),
                    Long.parseLong(highPrice),
                    Long.parseLong(lowPrice),
                    Long.parseLong(volume)
            );
        } catch (Exception e) {
            log.error("KIS 응답 파싱 실패 stockCode={}: {}", stockCode, e.getMessage());
            return null;
        }
    }

    public record PriceInfo(
            String stockCode,
            long currentPrice,
            long changePrice,
            double changeRate,
            long highPrice,
            long lowPrice,
            long volume
    ) {
        public String changeEmoji() {
            if (changeRate > 0) return "📈";
            if (changeRate < 0) return "📉";
            return "➡️";
        }

        public String formattedChangeRate() {
            return String.format("%+.2f%%", changeRate);
        }

        public String formattedCurrentPrice() {
            return String.format("%,d원", currentPrice);
        }
    }
}
