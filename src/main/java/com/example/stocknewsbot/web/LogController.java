package com.example.stocknewsbot.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LogController {
    private final LogFileReader logFileReader;

    public LogController(LogFileReader logFileReader) {
        this.logFileReader = logFileReader;
    }

    @GetMapping("/logs")
    public String logs(Model model, HttpServletRequest request) {
        model.addAttribute("title", "로그");
        model.addAttribute("activeTab", "logs");
        model.addAttribute("autoRefresh", true);
        model.addAttribute("content", "logs-content");

        model.addAttribute("logLines", logFileReader.readRecentLines());

        return FragmentViewResolver.resolve(request, "logs-content", "logs-content");
    }
}
