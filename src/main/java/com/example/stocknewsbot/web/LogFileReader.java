package com.example.stocknewsbot.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Component
public class LogFileReader {
    private static final Logger log = LoggerFactory.getLogger((LogFileReader.class));
    private static final String LOG_PATH = "logs/stock-news-bot.log";
    private static final int MAX_LINES = 300;

    public List<String> readRecentLines() {
        Path path = Path.of(LOG_PATH);
        if (!Files.exists(path)) {
            return List.of("로그 파일이 아직 생성되지 않았습니다.");
        }

        Deque<String> lines = new ArrayDeque<>();

        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            long pointer = file.length() - 1;
            StringBuilder lineBuilder = new StringBuilder();

            while (pointer >= 0 && lines.size() < MAX_LINES) {
                file.seek(pointer);
                int readByte = file.read();

                if (readByte == '\n') {
                    if (!lineBuilder.isEmpty()) {
                        lines.addFirst(lineBuilder.reverse().toString());
                        lineBuilder.setLength(0);
                    }
                } else if (readByte != '\r') {
                    lineBuilder.append((char) readByte);
                }
                pointer--;
            }

            // 파일 맨 앞까지 도달했는데 마지막 줄이 남아있는 경우
            if (!lineBuilder.isEmpty() && lines.size() < MAX_LINES) {
                lines.addFirst(lineBuilder.reverse().toString());
            }

        } catch (IOException e) {
            log.error("로그 파일 읽기 실패: {}", e.getMessage());
            return List.of("로그 파일을 읽는 중 오류가 발생했습니다: " + e.getMessage());
        }

        // 최신 로그가 위로 오도록
        return lines.stream().sorted((a,b) -> -1).toList().reversed();
    }

}
