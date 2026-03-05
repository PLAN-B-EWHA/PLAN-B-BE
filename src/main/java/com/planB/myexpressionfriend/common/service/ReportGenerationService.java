package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.report.GeneratedReport;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.report.ReportPreference;
import com.planB.myexpressionfriend.common.dto.report.GeneratedReportDTO;
import com.planB.myexpressionfriend.common.event.ReportGeneratedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportGenerationService {

    private final ReportPreferenceService reportPreferenceService;
    private final GeneratedReportService generatedReportService;
    private final ReportLlmClient reportLlmClient;
    private final ChildAuthorizationService childAuthorizationService;
    private final ApplicationEventPublisher eventPublisher;
    private final OpsMetricService opsMetricService;
    private static final Duration FAILURE_BACKOFF = Duration.ofMinutes(10);

    @Transactional
    public GeneratedReportDTO generateTestReport(
            UUID userId,
            UUID targetChildId,
            String promptOverride,
            Integer maxTokens
    ) {
        ReportPreference preference = reportPreferenceService.getOrCreate(userId);
        UUID resolvedChildId = resolveChildId(preference, targetChildId);
        validateViewReportPermission(userId, resolvedChildId);
        int resolvedMaxTokens = resolveMaxTokens(preference, maxTokens);
        String prompt = resolvePrompt(preference, resolvedChildId, promptOverride);

        GeneratedReport pending = generatedReportService.createPendingReport(
                userId,
                resolvedChildId,
                preference,
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now()
        );

        try {
            String reportText = reportLlmClient.generateReport(prompt, resolvedMaxTokens, preference.getModelName());

            GeneratedReport generated = generatedReportService.markGenerated(
                    pending.getReportId(),
                    buildTitle(preference),
                    buildSummary(reportText),
                    reportText,
                    prompt,
                    preference.getModelName(),
                    LocalDateTime.now()
            );

            LocalDateTime issuedAt = generated.getIssuedAt();
            LocalDateTime nextIssueAt = reportPreferenceService.calculateNextIssueAt(preference, issuedAt);
            reportPreferenceService.markIssued(userId, issuedAt, nextIssueAt);
            eventPublisher.publishEvent(new ReportGeneratedEvent(generated.getReportId(), userId));
            return GeneratedReportDTO.from(generated);
        } catch (Exception e) {
            opsMetricService.incrementReportGenerationFailure();
            generatedReportService.markFailed(pending.getReportId(), e.getMessage());
            throw e;
        }
    }

    @Transactional
    public GeneratedReportDTO generateScheduledReport(ReportPreference preference) {
        UUID userId = preference.getUserId();
        UUID resolvedChildId = preference.getTargetChildId();
        validateViewReportPermission(userId, resolvedChildId);
        int resolvedMaxTokens = preference.getMaxTokens();
        String prompt = resolvePrompt(preference, resolvedChildId, null);

        GeneratedReport pending = generatedReportService.createPendingReport(
                userId,
                resolvedChildId,
                preference,
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now()
        );

        try {
            String reportText = reportLlmClient.generateReport(prompt, resolvedMaxTokens, preference.getModelName());

            GeneratedReport generated = generatedReportService.markGenerated(
                    pending.getReportId(),
                    buildTitle(preference),
                    buildSummary(reportText),
                    reportText,
                    prompt,
                    preference.getModelName(),
                    LocalDateTime.now()
            );

            LocalDateTime issuedAt = generated.getIssuedAt();
            LocalDateTime nextIssueAt = reportPreferenceService.calculateNextIssueAt(preference, issuedAt);
            reportPreferenceService.markIssued(userId, issuedAt, nextIssueAt);
            eventPublisher.publishEvent(new ReportGeneratedEvent(generated.getReportId(), userId));
            return GeneratedReportDTO.from(generated);
        } catch (Exception e) {
            opsMetricService.incrementReportGenerationFailure();
            generatedReportService.markFailed(pending.getReportId(), e.getMessage());
            reportPreferenceService.postponeNextIssue(userId, LocalDateTime.now().plus(FAILURE_BACKOFF));
            throw e;
        }
    }

    private UUID resolveChildId(ReportPreference preference, UUID requestChildId) {
        if (requestChildId != null) {
            return requestChildId;
        }
        UUID targetChildId = preference.getTargetChildId();
        if (targetChildId == null) {
            throw new IllegalStateException("targetChildId is required for report generation");
        }
        return targetChildId;
    }

    private void validateViewReportPermission(UUID userId, UUID childId) {
        if (childId == null) {
            throw new IllegalStateException("targetChildId is required for report generation");
        }
        boolean hasPermission = childAuthorizationService.hasPermission(childId, userId, ChildPermissionType.VIEW_REPORT);
        if (!hasPermission) {
            throw new org.springframework.security.access.AccessDeniedException("리포트 조회 권한(VIEW_REPORT)이 없습니다.");
        }
    }

    private int resolveMaxTokens(ReportPreference preference, Integer requestMaxTokens) {
        if (requestMaxTokens != null && requestMaxTokens > 0) {
            return requestMaxTokens;
        }
        return preference.getMaxTokens();
    }

    private String resolvePrompt(ReportPreference preference, UUID targetChildId, String promptOverride) {
        if (promptOverride != null && !promptOverride.isBlank()) {
            return promptOverride.trim();
        }
        if (preference.getPromptTemplate() != null && !preference.getPromptTemplate().isBlank()) {
            return preference.getPromptTemplate();
        }

        String childInfo = targetChildId != null ? targetChildId.toString() : "ALL_CHILDREN";
        return """
                당신은 발달 치료 리포트를 작성하는 임상 지원 어시스턴트입니다.
                아래 정보를 바탕으로 보호자가 이해하기 쉬운 한국어 리포트를 작성하세요.
                - 대상 아동: %s
                - 기간: 최근 7일
                필수 섹션:
                1) 관찰 요약
                2) 강점
                3) 주의가 필요한 신호
                4) 가정에서의 권장 훈련 3가지
                5) 다음 주 체크 포인트
                주의사항:
                - 의학적 진단 확정 표현은 피하고 관찰 기반으로 작성
                - 과도한 단정 문구 금지
                """.formatted(childInfo);
    }

    private String buildTitle(ReportPreference preference) {
        return "자동 분석 리포트 (" + preference.getScheduleType().name() + ")";
    }

    private String buildSummary(String reportBody) {
        if (reportBody == null || reportBody.isBlank()) {
            return null;
        }
        int summaryLength = Math.min(reportBody.length(), 200);
        return reportBody.substring(0, summaryLength);
    }
}
