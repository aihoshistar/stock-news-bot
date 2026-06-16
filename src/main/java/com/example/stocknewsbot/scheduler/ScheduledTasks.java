package com.example.stocknewsbot.scheduler;

import com.example.stocknewsbot.dart.CorpCodeService;
import com.example.stocknewsbot.domain.SentNewsRepository;
import com.example.stocknewsbot.news.NewsService;
import com.example.stocknewsbot.price.PriceService;
import com.example.stocknewsbot.price.VolatilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final NewsService newsService;
    private final PriceService priceService;
    private final VolatilityService volatilityService;
    private final CorpCodeService corpCodeService;

    private final SentNewsRepository sentNewsRepository;

    public ScheduledTasks(NewsService newsService,
                          SentNewsRepository sentNewsRepository,
                          PriceService priceService,
                          VolatilityService volatilityService,
                          CorpCodeService corpCodeService) {
        this.newsService = newsService;
        this.sentNewsRepository = sentNewsRepository;
        this.priceService = priceService;
        this.volatilityService = volatilityService;
        this.corpCodeService = corpCodeService;
    }

    // 5분마다 뉴스 폴링
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void pollNews() {
        log.debug("뉴스 폴링 시작");
        newsService.pollAndSend();
    }

    // 매일 새벽 3시 — 30일 이상 된 발송 이력 정리
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupSentNews() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        sentNewsRepository.deleteOlderThan(cutoff);
        log.info("발송 이력 정리 완료 (30일 이상)");
    }

    // 평일 장중(09:00~15:30) 30분마다 시세 발송
    @Scheduled(cron = "0 0/30 9-15 * * MON-FRI")
    public void sendScheduledPrice() {
        log.debug("정기 시세 발송 시작");
        priceService.sendScheduledPrice();
    }

    // 평일 장중 2분마다 급변동 체크
    @Scheduled(cron = "0 0/2 9-15 * * MON-FRI")
    public void checkVolatility() {
        log.debug("급변동 체크 시작");
        volatilityService.checkVolatility();
    }

    // 매주 일요일 새벽 2시 기업코드 업데이트
    @Scheduled(cron = "0 0 2 * * SUN")
    public void refreshCorpCode() {
        log.info("DART 기업코드 업데이트 시작");
        corpCodeService.refresh();
    }
}
