package com.example.stocknewsbot.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TelegramBotService {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);

    private final TelegramClient telegramClient;
    private long offset = 0;

    public TelegramBotService(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startPolling() {
        Thread.ofVirtual().name("telegram-polling").start(this::pollingLoop);
        log.info("텔레그름 롱 폴링 시작");
    }

    private void pollingLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<Map<String, Object>> updates = telegramClient.getUpdates(offset);
                for (Map<String, Object> update : updates) {
                    handleUpdate(update);
                    offset = ((Number) update.get("update_id")).longValue() + 1;
                }
            } catch (Exception e) {
                log.error("폴링 에라: {}", e.getMessage());
                sleep(5_000);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleUpdate(Map<String, Object> update) {
        Map<String, Object> message = (Map<String, Object>) update.get("message");
        if (message == null) return;

        Map<String, Object> chat = (Map<String, Object>) message.get("chat");
        long chatId = ((Number) chat.get("id")).longValue();
        String text = (String) message.getOrDefault("text", "");

        log.debug("수신 chatId={} text={}", chatId, text);

        if (text.startsWith("/start")) {
            sendHelp(chatId);
        } else {
            telegramClient.sendMessage(chatId,"알 수 없는 명령입니다. /start 으로 도움말을 확인하세요");
        }
    }

    private void sendHelp(long chatId) {
        String help = """
                <b>주식 뉴스 알림 봇</b>
                <b>명령어 목록</b>
                /add [종목코드] [종목명] — 종목 구독
                /remove [종목코드] — 구독 취소
                /list — 구독 종목 목록
                /price [종목코드] — 현재가 조회
                /start — 도움말
                """;
        telegramClient.sendMessage(chatId, help);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
