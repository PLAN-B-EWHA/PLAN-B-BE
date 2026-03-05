package com.planB.myexpressionfriend.common.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OpsMetricService {

    private final AtomicLong reportGenerationFailureCount = new AtomicLong(0);
    private final AtomicLong notificationSaveFailureCount = new AtomicLong(0);
    private final AtomicLong accessDeniedCount = new AtomicLong(0);

    public void incrementReportGenerationFailure() {
        reportGenerationFailureCount.incrementAndGet();
    }

    public void incrementNotificationSaveFailure() {
        notificationSaveFailureCount.incrementAndGet();
    }

    public void incrementAccessDenied() {
        accessDeniedCount.incrementAndGet();
    }

    public Map<String, Long> snapshot() {
        return Map.of(
                "reportGenerationFailureCount", reportGenerationFailureCount.get(),
                "notificationSaveFailureCount", notificationSaveFailureCount.get(),
                "accessDeniedCount", accessDeniedCount.get()
        );
    }
}
