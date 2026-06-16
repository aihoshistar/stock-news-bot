package com.example.stocknewsbot.common;

import com.example.stocknewsbot.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class ErrorNotifier {
    private static final Logger log = LoggerFactory.getLogger(ErrorNotifier.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestClient restClient;
    private final String webhookUrl;

    public ErrorNotifier(AppProperties appProperties) {
        this.webhookUrl = appProperties.discord().webhookUrl();
        this.restClient = RestClient.builder().build();
    }

    /**
     * Discord로 에러 알림 전송
     * @param title     에러 제목
     * @param message   에러 내용
     * @param error     발생한 예외 (nullable)
     */
    public void notify(String title, String message, Throwable error) {
        log.error("[{}] {} - {}", title, message, error != null ? error.getMessage() : "");

        if (webhookUrl == null || webhookUrl.isBlank()) return;

        try {
            String errorDetail = error != null
                    ? "\n```" + error.getClass().getSimpleName()
                      + ": " + error.getMessage() + "```"
                    : "";

            String timestamp = LocalDateTime.now().format(FORMATTER);

            Map<String, Object> embed = Map.of(
                    "title", title,
                    "description", message + errorDetail,
                    "color", 15158332,  // 빨간색
                    "footer", Map.of("text", "Stock News Bot • " + timestamp)
            );

            restClient.post().uri(webhookUrl).body(Map.of("embed", List.of(embed))).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.error("Discord 알림 전송 실패: {}", e.getMessage());
        }
    }

    /**
     * 단순 메세지만 전송할 때 사용
     * @param title     제목
     * @param message   내용
     */
    public void notify(String title, String message) {
        notify(title, message, null);
    }
}
