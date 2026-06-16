package com.example.stocknewsbot.dart;

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
public class DartService {
    private static final Logger log = LoggerFactory.getLogger(DartService.class);
    private static final String DART_URL = "https://dart.fss.or.kr/dsaf001/main.do?rcpNo=";

    private final DartClient dartClient;
    private final CorpCodeService corpCodeService;
    private final SentNewsRepository sentNewsRepository;
    private final SubscriptionService subscriptionService;
    private final TelegramClient telegramClient;

    public DartService(DartClient dartClient,
                       CorpCodeService corpCodeService,
                       SentNewsRepository sentNewsRepository,
                       SubscriptionService subscriptionService,
                       TelegramClient telegramClient) {
        this.dartClient = dartClient;
        this.corpCodeService = corpCodeService;
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
        String corpCode = corpCodeService.getCorpCode(subscription.getStockCode());
        if (corpCode == null) {
            log.warn("corp_code 없음 stockCode={}", subscription.getStockCode());
            return;
        }

        // 최근 1일 공시 조회
        List<Map<String, Object>> disclosures =
                dartClient.getDisclosures(corpCode, 1);

        for (Map<String, Object> disclosure : disclosures) {
            String rcptNo = (String) disclosure.get("rcept_no");
            if (rcptNo == null) continue;

            // 접수번호를 중복 방지 키로 사용
            String linkHash = TextUtil.sha256(rcptNo);
            if (sentNewsRepository.existsByLinkHash(linkHash)) continue;

            String reportName = (String) disclosure.get("report_nm");
            String rcptDate  = (String) disclosure.get("rcept_dt");
            String link = DART_URL + rcptNo;

            String message = buildMessage(subscription, reportName, rcptDate, link);
            telegramClient.sendMessage(subscription.getChatId(), message);

            sentNewsRepository.save(
                    new SentNews(linkHash, subscription.getStockCode()));

            log.debug("공시 발송 stockCode={} report={}",
                    subscription.getStockCode(), reportName);
        }
    }

    private String buildMessage(Subscription subscription,
                                String reportName,
                                String rcptDate,
                                String link) {
        // rcptDate: "20240615" → "2024-06-15"
        String formattedDate = rcptDate.length() == 8
                ? rcptDate.substring(0, 4) + "-"
                  + rcptDate.substring(4, 6) + "-"
                  + rcptDate.substring(6, 8)
                : rcptDate;

        return "<b>" + subscription.getStockName()
                + "</b> (" + subscription.getStockCode() + ") 공시\n\n"
                + "<b>" + TextUtil.escapeHtml(reportName) + "</b>\n"
                + "접수일: " + formattedDate + "\n\n"
                + "<a href=\"" + link + "\">공시 보기</a>";
    }
}
