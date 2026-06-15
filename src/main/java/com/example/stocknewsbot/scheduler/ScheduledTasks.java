package com.example.stocknewsbot.scheduler;

import com.example.stocknewsbot.domain.SentNewsRepository;
import com.example.stocknewsbot.news.NewsService;
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
    private final SentNewsRepository sentNewsRepository;

    public ScheduledTasks(NewsService newsService, SentNewsRepository sentNewsRepository) {
        this.newsService = newsService;
        this.sentNewsRepository = sentNewsRepository;
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
}
