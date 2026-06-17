package com.example.stocknewsbot.telegram;

import com.example.stocknewsbot.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@ConditionalOnExpression("'${app.telegram.webhook-url:}'.length() > 0")
public class TelegramWebhookController {
    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookController.class);

    private final TelegramBotService telegramBotService;
    private final String secretToken;

    public TelegramWebhookController(TelegramBotService telegramBotService, AppProperties appProperties) {
        this.telegramBotService = telegramBotService;
        this.secretToken = appProperties.telegram().webhookSecretToken();
    }

    @PostMapping("/webhook/telegram")
    public ResponseEntity<Void> handleUpdate(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String incomingToken,
            @RequestBody Map<String, Object> update
    ) {
        if (secretToken != null && !secretToken.isBlank()) {
            if (!secretToken.equals(incomingToken)) {
                log.warn("Webhook Secret Token 일치하지 않음");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        log.debug("Webhook 업데이트 수신: {}", update.get("update_id"));
        telegramBotService.handleUpdate(update);
        return ResponseEntity.ok().build();
    }
}
