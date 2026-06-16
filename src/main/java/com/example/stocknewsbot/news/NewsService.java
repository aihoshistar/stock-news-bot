package com.example.stocknewsbot.news;

import com.example.stocknewsbot.ai.AiClient;
import com.example.stocknewsbot.ai.AiClient.NewsAnalysis;
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
    private final AiClient aiClient;
    private final TfIdfSimilarityFilter tfIdfSimilarityFilter;

    public NewsService(NaverNewsClient naverNewsClient,
                       SentNewsRepository sentNewsRepository,
                       SubscriptionService subscriptionService,
                       TelegramClient telegramClient,
                       AiClient aiClient,
                       TfIdfSimilarityFilter tfIdfSimilarityFilter
                       ) {
        this.naverNewsClient = naverNewsClient;
        this.sentNewsRepository = sentNewsRepository;
        this.subscriptionService = subscriptionService;
        this.telegramClient = telegramClient;
        this.aiClient = aiClient;
        this.tfIdfSimilarityFilter = tfIdfSimilarityFilter;
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
        List<Map<String, Object>> newsList =
                naverNewsClient.searchNews(subscription.getStockName());

        // 1. 중복 발송 이력 필터
        List<Map<String, Object>> unseenNews = newsList.stream()
                .filter(news -> {
                    String link = (String) news.get("link");
                    return link != null
                            && !sentNewsRepository.existsByLinkHash(TextUtil.sha256(link));
                })
                .toList();

        if (unseenNews.isEmpty()) return;

        // 2. TF-IDF 코사인 유사도를 사용해 유사 뉴스 필터
        List<String> titles = unseenNews.stream()
                .map(news -> TextUtil.stripHtml((String) news.getOrDefault("title", "")))
                .toList();

        List<Integer> selectedIndices = tfIdfSimilarityFilter.filterSimilar(titles);
        int filtered = unseenNews.size() - selectedIndices.size();
        if (filtered > 0) {
            log.debug("유사 뉴스 제거 stockCode={} 제거={}건",
                    subscription.getStockCode(), filtered);
        }

        // 3. 선택된 뉴스만 발송
        for (int idx : selectedIndices) {
            Map<String, Object> news = unseenNews.get(idx);
            String link = (String) news.get("link");
            String title = TextUtil.escapeHtml(titles.get(idx));

            NewsAnalysis analysis = aiClient.analyze(
                    subscription.getStockName(), title);

            telegramClient.sendMessage(subscription.getChatId(),
                    buildMessage(subscription, title, link, analysis));

            sentNewsRepository.save(
                    new SentNews(TextUtil.sha256(link), subscription.getStockCode()));

            log.debug("뉴스 발송 stockCode={} sentiment={} title={}",
                    subscription.getStockCode(), analysis.sentiment(), title);
        }
    }

    private String buildMessage(Subscription subscription, String title, String link, NewsAnalysis analysis) {
        return "<b>" + subscription.getStockName()
                + "</b> (" + subscription.getStockCode() + ") "
                + analysis.sentiment().emoji() + "\n\n"
                + "<b>" + title + "</b>\n\n"
                + analysis.summary() + "\n\n"
                + "<a href=\"" + link + "\">기사 읽기</a>";
    }
}
