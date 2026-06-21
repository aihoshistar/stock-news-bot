package com.example.stocknewsbot;

import com.example.stocknewsbot.domain.PriceHistoryRepository;
import com.example.stocknewsbot.domain.SentNewsRepository;
import com.example.stocknewsbot.domain.SubscriptionRepository;
import com.example.stocknewsbot.domain.VolatilityAlertRepository;
import com.example.stocknewsbot.telegram.TelegramHealthManager;
import com.example.stocknewsbot.web.FragmentViewResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
public class DashboardController {
    private final SubscriptionRepository subscriptionRepository;
    private final SentNewsRepository sentNewsRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final VolatilityAlertRepository volatilityAlertRepository;
    private final TelegramHealthManager telegramHealthManager;

    public DashboardController(SubscriptionRepository subscriptionRepository, SentNewsRepository sentNewsRepository, PriceHistoryRepository priceHistoryRepository, VolatilityAlertRepository volatilityAlertRepository, TelegramHealthManager telegramHealthManager) {
        this.subscriptionRepository = subscriptionRepository;
        this.sentNewsRepository = sentNewsRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.volatilityAlertRepository = volatilityAlertRepository;
        this.telegramHealthManager = telegramHealthManager;
    }

    @GetMapping("/")
    public String dashboard(Model model, HttpServletRequest request) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        model.addAttribute("title", "대시보드");
        model.addAttribute("activeTab", "dashboard");
        model.addAttribute("content", "dashboard-content");

        model.addAttribute("subscriptionCount", subscriptionRepository.count());
        model.addAttribute("todayNewsCount", sentNewsRepository.countBySentAtAfter(todayStart));
        model.addAttribute("todayPriceCount", priceHistoryRepository.countByQueriedAtAfter(todayStart));
        model.addAttribute("todayVolatilityCount", volatilityAlertRepository.countByOccurredAtAfter(todayStart));
        model.addAttribute("telegramStatus", telegramHealthManager.getStatusText());

        return FragmentViewResolver.resolve(request, "dashboard-content", "dashboard-content");
    }
}