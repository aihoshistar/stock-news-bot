package com.example.stocknewsbot.dart;

import com.example.stocknewsbot.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

@Service
public class CorpCodeService {
    private static final Logger log = LoggerFactory.getLogger(CorpCodeService.class);
    private static final String BASE_URL = "https://opendart.fss.or.kr";

    private final RestClient restClient;
    private final String apiKey;

    // stockCode 메모리 캐시
    private Map<String, String> corpCodeMap = new HashMap<>();

    public CorpCodeService(AppProperties appProperties) {
        this.apiKey = appProperties.dart().apiKey();
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

    public String getCorpCode(String stockCode) {
        if (corpCodeMap.isEmpty()) {
            refresh();
        }
        return corpCodeMap.get(stockCode);
    }

    public void refresh() {
        log.info("DART 기업코드 조회 시작");
        try {
            byte[] zipBytes = restClient.get()
                    .uri("/api/corpCode.xml?crtfc_key={key}", apiKey)
                    .retrieve()
                    .body(byte[].class);

            if (zipBytes == null) {
                log.error("DART 기업코드 ZIP 다운로드 실패");
                return;
            }

            Map<String, String> newMap = parseZip(zipBytes);
            corpCodeMap = newMap;
            log.info("DART 기업코드 조회 완료: {}개 기업", newMap.size());

        } catch (Exception e) {
            log.error("DART 기업코드 조회 실패: {}", e.getMessage());
        }
    }

    private Map<String, String> parseZip(byte[] zipBytes) throws Exception {
        Map<String, String> map = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(
                new ByteArrayInputStream(zipBytes))) {

            // ZIP 안의 첫 번째 파일(CORPCODE.xml)
            zis.getNextEntry();

            DocumentBuilder builder =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(zis);

            NodeList list = doc.getElementsByTagName("list");
            for (int i = 0; i < list.getLength(); i++) {
                var node = list.item(i);
                String corpCode = getTagValue(node, "corp_code");
                String stockCode = getTagValue(node, "stock_code");

                // 비상장 기업은 stock_code 가 없음
                if (stockCode != null && !stockCode.isBlank()) {
                    map.put(stockCode.trim(), corpCode.trim());
                }
            }
        }
        return map;
    }

    private String getTagValue(org.w3c.dom.Node node, String tagName) {
        var element = (org.w3c.dom.Element) node;
        var tag = element.getElementsByTagName(tagName);
        if (tag.getLength() == 0) return null;
        return tag.item(0).getTextContent();
    }
}
