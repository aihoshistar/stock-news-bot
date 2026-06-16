package com.example.stocknewsbot.price;

import com.example.stocknewsbot.config.AppProperties;
import com.example.stocknewsbot.domain.Subscription;
import com.example.stocknewsbot.price.KisClient.PriceInfo;
import com.example.stocknewsbot.subscription.SubscriptionService;
import com.example.stocknewsbot.telegram.TelegramClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VolatilityService {
    private static final Logger log = LoggerFactory.getLogger(VolatilityService.class);
    private final KisClient kisClient;
    private final SubscriptionService subscriptionService;
    private final TelegramClient telegramClient;
    private final double threshold;

    // 메모리 캐싱
    private final Map<String, Long> basePrice = new ConcurrentHashMap<>();

    public VolatilityService(KisClient kisClient, SubscriptionService subscriptionService, TelegramClient telegramClient, AppProperties appProperties) {
        this.kisClient = kisClient;
        this.subscriptionService = subscriptionService;
        this.telegramClient = telegramClient;
        this.threshold = appProperties.alert().volatilityThreshold();
    }

    public void checkVolatility() {
        List<Subscription> subscriptions = subscriptionService.findAll();
        if (subscriptions.isEmpty()) return;

        for (Subscription subscription : subscriptions) {
            checkStock(subscription);
        }
    }

    private void checkStock(Subscription subscription) {
        String stockCode = subscription.getStockCode();
        PriceInfo info = kisClient.getCurrentPrice(stockCode);
        if (info == null) return;

        long current = info.currentPrice();
        Long base = basePrice.get(stockCode);

        if (base == null) {
            basePrice.put(stockCode, current);
            log.debug("기준가 등록 stockCode={} basePrice={}", stockCode, current);
            return;
        }

        double changeRate = (double) (current - base) / base * 100;

        if (Math.abs(changeRate) >= threshold) {
            sendAlert(subscription, info, base, changeRate);
            basePrice.put(stockCode, current);
        }
    }

    private void sendAlert(Subscription subscription, PriceInfo priceInfo, long basePrice, double changeRate) {
        String direction = changeRate > 0 ? "급등" : "급락";
        String message = "<b>" + subscription.getStockName()
                + "</b> (" + subscription.getStockCode() + ") " + direction + " 알림\n\n"
                + "현재가: <b>" + priceInfo.formattedCurrentPrice() + "</b>\n"
                + "기준가: " + String.format("%,d원", basePrice) + "\n"
                + "변동률: <b>" + String.format("%+.2f%%", changeRate) + "</b>";

        telegramClient.sendMessage(subscription.getChatId(), message);
        log.info("급변동 알림 발송 stockCode={} changeRate={}%", subscription.getStockCode(), String.format("%.2f", changeRate));
    }
}
