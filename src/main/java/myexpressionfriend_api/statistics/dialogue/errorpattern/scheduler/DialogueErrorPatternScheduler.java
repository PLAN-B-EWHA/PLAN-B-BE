package myexpressionfriend_api.statistics.dialogue.errorpattern.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.statistics.dialogue.errorpattern.service.DialogueErrorPatternBatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class DialogueErrorPatternScheduler {

    private final DialogueErrorPatternBatchService batchService;

    // 기본은 매주 월요일 03:00 실행, 홀수 주차에서만 실제 배치 수행
    @Scheduled(cron = "${statistics.error-pattern.cron:0 0 3 * * MON}")
    public void runBiweeklyErrorPatternBatch() {
        int weekOfYear = LocalDate.now().get(WeekFields.of(Locale.KOREA).weekOfWeekBasedYear());
        if (weekOfYear % 2 == 0) {
            log.info("Skip error pattern batch on even week. week={}", weekOfYear);
            return;
        }

        log.info("Start error pattern batch. week={}", weekOfYear);
        batchService.runBiweeklyBatch();
        log.info("Finish error pattern batch. week={}", weekOfYear);
    }
}
