package com.example.stocknewsbot.news;

import com.example.stocknewsbot.common.TextUtil;
import com.example.stocknewsbot.domain.SentNews;
import com.example.stocknewsbot.domain.SentNewsRepository;
import com.example.stocknewsbot.domain.Subscription;
import com.example.stocknewsbot.subscription.SubscriptionService;
import com.example.stocknewsbot.telegram.TelegramClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class NewsService {
    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    private final NaverNewsClient naverNewsClient;
    private final SentNewsRepository sentNewsRepository;
    private final SubscriptionService subscriptionService;
    private final TelegramClient telegramClient;

    public NewsService(NaverNewsClient naverNewsClient,
                       SentNewsRepository sentNewsRepository,
                       SubscriptionService subscriptionService,
                       TelegramClient telegramClient) {
        this.naverNewsClient = naverNewsClient;
        this.sentNewsRepository = sentNewsRepository;
        this.subscriptionService = subscriptionService;
        this.telegramClient = telegramClient;
    }

    @Transactional
    public void pollAndSend() {
        List<Subscription> subscriptions = subscriptionService.findAll();
        if (subscriptions.isEmpty()) return;

        for (Subscription subscription : subscriptions) {
            processSubscription(subscription);
        }
    }

    private void processSubscription(Subscription subscription) {
        List<Map<String, Object>> newsList = naverNewsClient.searchNews(subscription.getStockName());

        for (Map<String, Object> news : newsList) {
            String link = (String) news.get("link");
            if (link == null) continue;

            String linkHash = TextUtil.sha256(link);

            if (sentNewsRepository.existsAllByLinkHash(linkHash)) continue;

            String title = TextUtil.escapeHtml(
                    TextUtil.stripHtml((String) news.get("title"))
            );
            String message = buildMessage(subscription, title, link);
            telegramClient.sendMessage(subscription.getChatId(), message);

            sentNewsRepository.save(new SentNews(linkHash, subscription.getStockCode()));

            log.debug("뉴스 발송 stockCode={} title={}", subscription.getStockCode(), title);
        }
    }

    private String buildMessage(Subscription subscription, String title, String link) {
        return "<b>" + subscription.getStockName()
                + "</b> (" + subscription.getStockCode() + ")\n\n"
                + title + "\n\n"
                + "<a href=\"" + link + "\">기사 읽기</a>";
    }
}
