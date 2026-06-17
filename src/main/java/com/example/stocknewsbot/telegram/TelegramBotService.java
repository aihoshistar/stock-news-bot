package com.example.stocknewsbot.telegram;

import com.example.stocknewsbot.config.AppProperties;
import com.example.stocknewsbot.domain.Subscription;
import com.example.stocknewsbot.price.PriceService;
import com.example.stocknewsbot.subscription.SubscriptionService;
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
    private final SubscriptionService subscriptionService;
    private final PriceService priceService;
    private final TelegramHealthManager telegramHealthManager;

    private final boolean webhookMode;

    private boolean wasSleeping = false;
    private long offset = 0;

    public TelegramBotService(TelegramClient telegramClient, SubscriptionService subscriptionService, PriceService priceService, TelegramHealthManager telegramHealthManager, AppProperties appProperties) {
        this.telegramClient = telegramClient;
        this.subscriptionService = subscriptionService;
        this.priceService = priceService;
        this.telegramHealthManager = telegramHealthManager;
        this.webhookMode = appProperties.telegram().isWebhookMode();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startPolling() {
        if (webhookMode) {
            log.info("텔레그램 Webhook 모드로 동작 중");
            return;
        }
        Thread.ofVirtual().name("telegram-polling").start(this::pollingLoop);
        log.info("텔레그름 롱 폴링 시작");
    }

    private void pollingLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (telegramHealthManager.isSleeping()) {
                    wasSleeping = true;
                    sleep(60_000); // sleep 중 1분마다 체크
                    continue;
                }

                // sleep 해제 후 첫 성공 시 복구 알림
                if (wasSleeping) {
                    wasSleeping = false;
                    telegramHealthManager.notifyRecovery();
                }

                List<Map<String, Object>> updates = telegramClient.getUpdates(offset);
                for (Map<String, Object> update : updates) {
                    handleUpdate(update);
                    offset = ((Number) update.get("update_id")).longValue() + 1;
                }
            } catch (Exception e) {
                log.error("폴링 루프 오류: {}", e.getMessage());
                sleep(5_000);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void handleUpdate(Map<String, Object> update) {
        Map<String, Object> message = (Map<String, Object>) update.get("message");
        if (message == null) return;

        Map<String, Object> chat = (Map<String, Object>) message.get("chat");
        long chatId = ((Number) chat.get("id")).longValue();
        String text = (String) message.getOrDefault("text", "");

        log.debug("수신 chatId={} text={}", chatId, text);

        String[] parts = text.trim().split("\\s+");
        String command = parts[0].toLowerCase();

        switch (command) {
            case "/start"   -> sendHelp(chatId);
            case "/add"     -> handleAdd(chatId, parts);
            case "/remove"  -> handleRemove(chatId, parts);
            case "/list"    -> handleList(chatId);
            case "/price"   -> handlePrice(chatId, parts);
            default         -> telegramClient.sendMessage(chatId,"알 수 없는 명령입니다. /start 으로 도움말을 확인하세요");
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

    private void handleAdd(long chatId, String[] parts) {
        if (parts.length < 3) {
            telegramClient.sendMessage(chatId, "사용방법: /add [종목코드] [종목명 \n예) /add 041510 에스엠");
            return;
        }

        String stockCode = parts[1];
        String stockName = parts[2];

        boolean added = subscriptionService.add(chatId, stockCode, stockName);
        if (added) {
            telegramClient.sendMessage(chatId, "<b>" + stockName + "</b>(" + stockCode + ") 구독을 시작했습니다.");
        } else {
            telegramClient.sendMessage(chatId, "이미 구독중인 종목입니다: <b>" + stockName + "</b>(" + stockCode + ")");
        }
    }

    private void handleRemove(long chatId, String[] parts) {
        if (parts.length < 2) {
            telegramClient.sendMessage(chatId, "사용법: /remove [종목코드]\n예) /remove 041510");
            return ;
        }

        String stockCode = parts[1];

        boolean removed = subscriptionService.remove(chatId, stockCode);
        if (removed) {
            telegramClient.sendMessage(chatId, stockCode + " 구독을 취소했습니다.");
        } else {
            telegramClient.sendMessage(chatId, "구독중이 아닌 종목입니다: " + stockCode);
        }
    }

    private void handleList(long chatId) {
        List<Subscription> list = subscriptionService.list(chatId);
        if (list.isEmpty()) {
            telegramClient.sendMessage(chatId, "구독중인 종목이 없습니다. \n/add [종목코드] 종목명] 으로 추가하세요.");
            return;
        }

        StringBuilder sb = new StringBuilder("<b>구독 종목 목록</b>\n\n");
        for (Subscription s : list) {
            sb.append("* ").append(s.getStockName())
                    .append(" (").append(s.getStockCode()).append(")\n");
        }

        telegramClient.sendMessage(chatId, sb.toString());
    }

    private void handlePrice(long chatId, String[] parts) {
        if (parts.length < 2) {
            telegramClient.sendMessage(chatId, "사용법: /price [종목코드]\n예) /price 041510");
            return;
        }
        priceService.sendPrice(chatId, parts[1]);
    }
}
