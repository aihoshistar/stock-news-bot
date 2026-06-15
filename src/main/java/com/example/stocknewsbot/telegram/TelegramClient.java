package com.example.stocknewsbot.telegram;

import com.example.stocknewsbot.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class TelegramClient {
    private static final Logger log = LoggerFactory.getLogger(TelegramClient.class);
    private static final String BASE_URL = "https://api.telegram.org";

    private final RestClient restClient;
    private final String token;
    private final long pollingTimeout;

    public TelegramClient(AppProperties appProperties) {
        this.token = appProperties.telegram().token();
        this.pollingTimeout = appProperties.telegram().pollingTimeout();
        this.restClient = RestClient.builder().baseUrl(BASE_URL).build();
    }

    /**
     * 텔레그램 메세지 전송
     * @param chatId    수신 chat_id
     * @param text      전송 HTML 메세지
     */
    public void sendMessage(long chatId, String text) {
        try {
            restClient.post().uri("/bot{token}/sendMessage", token).body(Map.of(
                    "chat_id", chatId,
                    "text", text,
                    "parse_mode", "HTML"
            )).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.error("텔레그렘 메세지 전송 실패 chatId={}: {}", chatId, e.getMessage());
        }
    }

    /**
     * 텔레그램 업데이트 내용을 Long Polling 으로 가져옴
     * @param offset    마지막 update_id + 1
     * @return          업데이트 목록
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getUpdates(long offset) {
        try {
            Map<String, Object> response = restClient.get().uri("/bot{token}/getUpdates?offset={offset}&timeout={timeout}", token, offset, pollingTimeout).retrieve().body(Map.class);

            if (response != null && Boolean.TRUE.equals(response.get("ok"))) {
                return (List<Map<String, Object>>) response.get("result");
            }
        } catch (Exception e) {
            log.error("텔레그렘 업데이트 받기 실패: {}", e.getMessage());
        }

        return List.of();
    }
}
