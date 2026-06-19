package com.example.stocknewsbot.web;

import com.example.stocknewsbot.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class TimelineService {
    private final SentNewsRepository sentNewsRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final VolatilityAlertRepository volatilityAlertRepository;

    public TimelineService(SentNewsRepository sentNewsRepository, PriceHistoryRepository priceHistoryRepository, VolatilityAlertRepository volatilityAlertRepository) {
        this.sentNewsRepository = sentNewsRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.volatilityAlertRepository = volatilityAlertRepository;
    }

    public List<TimelineItem> getTimeline(LocalDateTime after) {
        Stream<TimelineItem> newsStream = sentNewsRepository
                .findBySentAtAfterOrderBySentAtDesc(after)
                .stream()
                .map(this::toTimelineItem);

        Stream<TimelineItem> priceStream = priceHistoryRepository
                .findByQueriedAtAfterOrderByQueriedAtDesc(after)
                .stream()
                .map(this::toTimelineItem);

        Stream<TimelineItem> volatilityStream = volatilityAlertRepository
                .findByOccurredAtAfterOrderByOccurredAtDesc(after)
                .stream()
                .map(this::toTimelineItem);

        return Stream.of(newsStream, priceStream, volatilityStream)
                .flatMap(s -> s)
                .sorted(Comparator.comparing(TimelineItem::occurredAt).reversed())
                .toList();
    }

    private TimelineItem toTimelineItem(SentNews news) {
        String stockName = news.getStockName() != null ? news.getStockName() : news.getStockCode();
        return new TimelineItem(
                news.getSentAt(),
                TimelineItem.TimelineType.NEWS,
                news.getStockCode(),
                stockName,
                "뉴스/공시 발송",
                "발송 이력 (상세 내용은 텔레그램 채널 참고)",
                null
        );
    }

    private TimelineItem toTimelineItem(PriceHistory price) {
        String direction = price.getChangeRate() > 0 ? "📈" : price.getChangeRate() < 0 ? "📉" : "➡️";
        return new TimelineItem(
                price.getQueriedAt(),
                TimelineItem.TimelineType.PRICE,
                price.getStockCode(),
                price.getStockName(),
                "시세 조회",
                direction + " " + String.format("%,d원 (%+.2f%%)",
                        price.getCurrentPrice(), price.getChangeRate()),
                null
        );
    }

    private TimelineItem toTimelineItem(VolatilityAlert alert) {
        String direction = alert.getChangeRate() > 0 ? "급등" : "급락";
        return new TimelineItem(
                alert.getOccurredAt(),
                TimelineItem.TimelineType.VOLATILITY,
                alert.getStockCode(),
                alert.getStockName(),
                direction + " 알림",
                String.format("%,d원 → %,d원 (%+.2f%%)",
                        alert.getBasePrice(), alert.getCurrentPrice(), alert.getChangeRate()),
                null
        );
    }
}
