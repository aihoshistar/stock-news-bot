package com.example.stocknewsbot.web;

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
    public String logs(Model model) {
        model.addAttribute("title", "로그");
        model.addAttribute("activeTab", "logs");
        model.addAttribute("autoRefresh", true);
        model.addAttribute("content", "logs-content");

        model.addAttribute("logLines", logFileReader.readRecentLines());

        return "layout";
    }
}
