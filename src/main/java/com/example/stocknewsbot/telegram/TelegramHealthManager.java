package com.example.stocknewsbot.telegram;

import com.example.stocknewsbot.common.ErrorNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TelegramHealthManager {
    private static final Logger log = LoggerFactory.getLogger(TelegramHealthManager.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final int FAILURE_THRESHOLD = 3;
    private static final long SLEEP_DURATION_MS = 12 * 60 * 60 * 1000L; // 12시간

    private final ErrorNotifier errorNotifier;

    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicBoolean sleeping = new AtomicBoolean(false);
    private volatile LocalDateTime sleepUntil = null;

    public TelegramHealthManager(ErrorNotifier errorNotifier) {
        this.errorNotifier = errorNotifier;
    }

    public void recordFailure() {
        int count = failureCount.incrementAndGet();
        log.warn("텔레그램 실패 카운트: {}/{}", count, FAILURE_THRESHOLD);

        if (count >= FAILURE_THRESHOLD && !sleeping.get()) {
            enterSleepMode();
        }
    }

    public void recordSuccess() {
        if (failureCount.get() > 0) {
            failureCount.set(0);
            log.debug("텔레그램 실패 카운트 초기화");
        }
    }

    public boolean isSleeping() {
        if (!sleeping.get()) return false;

        // sleep 시간이 지났으면 자동 해제 시도
        if (LocalDateTime.now().isAfter(sleepUntil)) {
            tryWakeUp();
        }
        return sleeping.get();
    }

    private void enterSleepMode() {
        sleeping.set(true);
        sleepUntil = LocalDateTime.now().plusHours(12);
        failureCount.set(0);

        String startTime = LocalDateTime.now().format(FORMATTER);
        String endTime = sleepUntil.format(FORMATTER);

        log.error("텔레그램 연결 실패 — {} ~ {} sleep 시작", startTime, endTime);

        errorNotifier.notify(
                "텔레그램 연결 실패 — Sleep 모드 진입",
                "텔레그램 응답이 없어 모든 작업을 일시 중단합니다.\n\n"
                        + "Sleep 시작: " + startTime + "\n"
                        + "재시작 예정: " + endTime
        );
    }

    private void tryWakeUp() {
        String wakeTime = LocalDateTime.now().format(FORMATTER);
        log.info("텔레그램 sleep 시간 종료 — 연결 재시도 중 ({})", wakeTime);

        // sleeping 해제 후 TelegramClient가 실제 호출 후 성공/실패를 다시 기록하도록 함
        sleeping.set(false);
        sleepUntil = null;
    }

    public void notifyRecovery() {
        String wakeTime = LocalDateTime.now().format(FORMATTER);
        log.info("텔레그램 연결 복구 확인 ({})", wakeTime);

        errorNotifier.notify(
                "텔레그램 연결 복구",
                "텔레그램 응답이 정상화되어 모든 작업을 재개합니다.\n\n"
                        + "복구 시간: " + wakeTime
        );
    }

    public String getStatusText() {
        return isSleeping() ?  "Sleep 중 (응답 없음)" : "정상";
    }
}
