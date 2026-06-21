package com.example.stocknewsbot.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
public class HistoryController {
    private final TimelineService timelineService;

    public HistoryController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping("/history")
    public String history(@RequestParam(defaultValue = "1") int days, Model model, HttpServletRequest request) {
        LocalDateTime after = LocalDateTime.now().minusDays(days);

        model.addAttribute("title", "발송 이력");
        model.addAttribute("activeTab", "history");
        model.addAttribute("autoRefresh", true);
        model.addAttribute("content", "history-content");

        model.addAttribute("timeline", timelineService.getTimeline(after));
        model.addAttribute("selectedDays", days);

        return FragmentViewResolver.resolve(request, "history-content", "history-content");
    }
}
