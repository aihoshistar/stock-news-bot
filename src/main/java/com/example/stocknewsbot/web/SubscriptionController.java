package com.example.stocknewsbot.web;

import com.example.stocknewsbot.domain.SubscriptionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class SubscriptionController {
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionController(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @GetMapping("/subscriptions")
    public String subscriptions(Model model, HttpServletRequest request) {
        List<StockSubscriptionView> views = new ArrayList<>();
        for (Object[] row : subscriptionRepository.countSubscribersByStock()) {
            views.add(new StockSubscriptionView(
                    (String) row[0],
                    (String) row[1],
                    (Long) row[2]
            ));
        }

        long totalStocks = views.size();
        long totalSubscriptions = views.stream()
                .mapToLong(StockSubscriptionView::subscriberCount)
                .sum();

        model.addAttribute("title", "구독 현황");
        model.addAttribute("activeTab", "subscriptions");
        model.addAttribute("autoRefresh", true);
        model.addAttribute("content", "subscriptions-content");


        model.addAttribute("stocks", views);
        model.addAttribute("totalStocks", totalStocks);
        model.addAttribute("totalSubscriptions", totalSubscriptions);

        return FragmentViewResolver.resolve(request, "subscriptions-content", "subscriptions-content");
    }

    public record StockSubscriptionView(String stockCode, String stockName, Long subscriberCount) {}
}
