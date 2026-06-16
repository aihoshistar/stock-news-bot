package com.example.stocknewsbot.dart;

import com.example.stocknewsbot.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class DartClient {
    private static final Logger log = LoggerFactory.getLogger(DartClient.class);
    private static final String BASE_URL = "https://opendart.fss.or.kr";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestClient restClient;
    private final String apiKey;

    public DartClient(AppProperties appProperties) {
        this.apiKey = appProperties.dart().apiKey();
        this.restClient = RestClient.builder().baseUrl(BASE_URL).build();
    }

    /**
     * 특정 기업의 최근 공시 목록 조회
     * @param corpCode  DART 고유 기업코드
     * @param days      조회 기간 (오늘부터 N일 까지)
     * @return          공시 목록
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDisclosures(String corpCode, int days) {
        String endDate = LocalDate.now().format(DATE_FORMATTER);
        String startDate = LocalDate.now().minusDays(days).format(DATE_FORMATTER);

        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/list.json")
                            .queryParam("crtfc_key", apiKey)
                            .queryParam("corp_code", corpCode)
                            .queryParam("bgn_de", startDate)
                            .queryParam("end_de", endDate)
                            .queryParam("sort", "date")
                            .queryParam("sort_mth", "desc")
                            .queryParam("page_count", "10")
                            .build())
                    .retrieve()
                    .body(Map.class);

            if (response == null) return List.of();

            // status "000" 이 정상, "013" 은 공시 없음
            String status = (String) response.get("status");
            if ("013".equals(status)) return List.of();
            if (!"000".equals(status)) {
                log.warn("DART API 오류 status={} corpCode={}", status, corpCode);
                return List.of();
            }

            return (List<Map<String, Object>>) response.get("list");
        } catch (Exception e) {
            log.error("DART 공시 조회 실패 corpCode={}: {}", corpCode, e.getMessage());
            return List.of();
        }
    }

}
