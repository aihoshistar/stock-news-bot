package com.example.stocknewsbot.price;

import com.example.stocknewsbot.domain.Subscription;
import com.example.stocknewsbot.price.KisClient.PriceInfo;
import com.example.stocknewsbot.subscription.SubscriptionService;
import com.example.stocknewsbot.telegram.TelegramClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class PriceService {
    private static final Logger log = LoggerFactory.getLogger(PriceService.class);

    private final KisClient kisClient;
    private final SubscriptionService subscriptionService;
    private final TelegramClient telegramClient;

    public PriceService(KisClient kisClient, SubscriptionService subscriptionService, TelegramClient telegramClient) {
        this.kisClient = kisClient;
        this.subscriptionService = subscriptionService;
        this.telegramClient = telegramClient;
    }

    /**
     * /price 에 입력된 종목의 현재 가격을 조회 후 전송
     */
    public void sendPrice(long chatid, String stockCode) {
        PriceInfo info = kisClient.getCurrentPrice(stockCode);
        if (info == null) {
            telegramClient.sendMessage(chatid, "시세 조회 실패: " + stockCode + "\n종목코드를 확인해주세요.");
            return;
        }
        telegramClient.sendMessage(chatid, buildMessage(stockCode, stockCode, info));
    }

    /**
     * 구독 종목의 현재 가격 전송
     */
    public void sendScheduledPrice() {
        List<Subscription> subscriptions = subscriptionService.findAll();
        if (subscriptions.isEmpty()) return ;

        for (Subscription subscription : subscriptions) {
            PriceInfo info = kisClient.getCurrentPrice(subscription.getStockCode());
            if (info == null) {
                log.warn("시세 조회 실패 stockCode={}", subscription.getStockCode());
                continue;
            }
            String message = buildMessage(
                    subscription.getStockCode(),
                    subscription.getStockCode(),
                    info
            );

            telegramClient.sendMessage(subscription.getChatId(), message);
            log.debug("시세 발송 stockCode={} price={}", subscription.getStockCode(), info.currentPrice());
        }
    }

    private String buildMessage(String stockCode, String stockName, PriceInfo info) {
        return info.changeEmoji() + " <b>" + stockName + "</b> (" + stockCode + ")\n\n"
                + "현재가: <b>" + info.formattedCurrentPrice() + "</b>\n"
                + "등락률: " + info.formattedChangeRate() + "\n"
                + "고가: " + String.format("%,d원", info.highPrice()) + "\n"
                + "저가: " + String.format("%,d원", info.lowPrice()) + "\n"
                + "거래량: " + String.format("%,d", info.volume());

    }
}
