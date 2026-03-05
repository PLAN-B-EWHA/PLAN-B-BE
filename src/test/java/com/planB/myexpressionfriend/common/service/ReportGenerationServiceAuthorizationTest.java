package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.report.GeneratedReport;
import com.planB.myexpressionfriend.common.domain.report.ReportPreference;
import com.planB.myexpressionfriend.common.dto.report.GeneratedReportDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportGenerationServiceAuthorizationTest {

    @Mock
    private ReportPreferenceService reportPreferenceService;
    @Mock
    private GeneratedReportService generatedReportService;
    @Mock
    private ReportLlmClient reportLlmClient;
    @Mock
    private ChildAuthorizationService childAuthorizationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReportGenerationService reportGenerationService;

    @Test
    @DisplayName("VIEW_REPORT 권한이 없으면 테스트 리포트 생성이 거부된다")
    void generateTestReport_withoutViewReport_throwsAccessDenied() {
        UUID userId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();

        ReportPreference preference = ReportPreference.builder()
                .userId(userId)
                .targetChildId(childId)
                .modelName("default")
                .maxTokens(1200)
                .build();

        when(reportPreferenceService.getOrCreate(userId)).thenReturn(preference);
        when(childAuthorizationService.hasPermission(
                childId,
                userId,
                com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT
        )).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> reportGenerationService.generateTestReport(userId, childId, null, null));

        verify(generatedReportService, never()).createPendingReport(any(), any(), any(), any(), any());
        verify(reportLlmClient, never()).generateReport(anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("VIEW_REPORT 권한이 있으면 테스트 리포트 생성이 허용된다")
    void generateTestReport_withViewReport_ok() {
        UUID userId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();

        ReportPreference preference = ReportPreference.builder()
                .userId(userId)
                .targetChildId(childId)
                .modelName("default")
                .maxTokens(1200)
                .build();

        GeneratedReport pending = GeneratedReport.builder()
                .reportId(reportId)
                .userId(userId)
                .targetChildId(childId)
                .build();

        GeneratedReport generated = GeneratedReport.builder()
                .reportId(reportId)
                .userId(userId)
                .targetChildId(childId)
                .build();
        generated.markGenerated(
                "title",
                "summary",
                "body",
                "prompt",
                "default",
                LocalDateTime.now()
        );

        when(reportPreferenceService.getOrCreate(userId)).thenReturn(preference);
        when(childAuthorizationService.hasPermission(
                childId,
                userId,
                com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT
        )).thenReturn(true);
        when(generatedReportService.createPendingReport(any(), any(), any(), any(), any())).thenReturn(pending);
        when(reportLlmClient.generateReport(anyString(), anyInt(), anyString())).thenReturn("body");
        when(generatedReportService.markGenerated(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(generated);
        when(reportPreferenceService.calculateNextIssueAt(any(), any())).thenReturn(LocalDateTime.now().plusDays(7));

        GeneratedReportDTO result = reportGenerationService.generateTestReport(userId, childId, null, null);

        assertNotNull(result);
        assertEquals(reportId, result.getReportId());
        verify(generatedReportService).createPendingReport(any(), any(), any(), any(), any());
        verify(reportLlmClient).generateReport(anyString(), anyInt(), anyString());
    }
}
