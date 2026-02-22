package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.report.ReportPreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportAutoPublishScheduler {

    private final ReportPreferenceService reportPreferenceService;
    private final ReportGenerationService reportGenerationService;
    private final Set<UUID> inProgressUsers = ConcurrentHashMap.newKeySet();

    @Value("${report.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Scheduled(fixedDelayString = "${report.scheduler.fixed-delay-ms:60000}")
    public void publishScheduledReports() {
        if (!schedulerEnabled) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<ReportPreference> targets = reportPreferenceService.findIssuablePreferences(now);
        if (targets.isEmpty()) {
            return;
        }

        log.info("Report scheduler tick. targets={}", targets.size());
        for (ReportPreference preference : targets) {
            UUID userId = preference.getUserId();
            if (!inProgressUsers.add(userId)) {
                continue;
            }

            try {
                reportGenerationService.generateScheduledReport(preference);
                log.info("Scheduled report generated. userId={}", userId);
            } catch (Exception e) {
                log.warn("Scheduled report generation failed. userId={}, reason={}", userId, e.getMessage());
            } finally {
                inProgressUsers.remove(userId);
            }
        }
    }
}
