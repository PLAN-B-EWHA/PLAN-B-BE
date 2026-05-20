package myexpressionfriend_api.report.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.auth.domain.user.User;
import myexpressionfriend_api.auth.repository.UserRepository;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.domain.ChildPermissionType;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.common.exception.AuthenticationFailedException;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import myexpressionfriend_api.report.domain.AiReport;
import myexpressionfriend_api.report.domain.ReportStatus;
import myexpressionfriend_api.report.dto.ReportGenerateDraftRequest;
import myexpressionfriend_api.report.dto.ReportResponse;
import myexpressionfriend_api.report.dto.ReportUpdateRequest;
import myexpressionfriend_api.report.repository.AiReportRepository;
import myexpressionfriend_api.rag.dto.RagGenerateRequest;
import myexpressionfriend_api.rag.dto.RagGenerateResponse;
import myexpressionfriend_api.rag.service.RagGenerationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ChildRepository childRepository;
    private final UserRepository userRepository;
    private final AiReportRepository aiReportRepository;
    private final RagGenerationService ragGenerationService;

    @Transactional
    public ReportResponse generateDraft(UUID userId, ReportGenerateDraftRequest request) {
        User user = loadUser(userId);
        Child child = loadChildWithAnyPermissionOrAdmin(
                user,
                request.childId(),
                ChildPermissionType.VIEW_REPORT,
                ChildPermissionType.ASSIGN_MISSION,
                ChildPermissionType.MANAGE
        );

        RagGenerateResponse generated = ragGenerationService.generateReport(new RagGenerateRequest(
                child.getChildId(),
                request.request(),
                request.retrievalQuery(),
                request.childSummary(),
                request.additionalContext(),
                request.templateKey(),
                request.topK(),
                request.similarityThreshold(),
                request.useProModel(),
                true
        ));

        AiReport report = AiReport.builder()
                .child(child)
                .generatedBy(user)
                .reportType(request.reportType())
                .status(ReportStatus.DRAFT)
                .title(resolveTitle(request.title(), request.reportType().name()))
                .content(generated.generatedText())
                .ragContextSnapshot(generated.ragContext())
                .promptSnapshot(generated.prompt())
                .model(generated.model())
                .build();

        return ReportResponse.from(aiReportRepository.save(report), true);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> getReportsForTherapist(
            UUID userId,
            UUID childId,
            ReportStatus status,
            Pageable pageable
    ) {
        User user = loadUser(userId);
        loadChildWithAnyPermissionOrAdmin(
                user,
                childId,
                ChildPermissionType.VIEW_REPORT,
                ChildPermissionType.ASSIGN_MISSION,
                ChildPermissionType.MANAGE
        );

        Page<AiReport> reports = status == null
                ? aiReportRepository.findByChild_ChildIdOrderByCreatedAtDesc(childId, pageable)
                : aiReportRepository.findByChild_ChildIdAndStatusOrderByCreatedAtDesc(childId, status, pageable);

        return reports.map(report -> ReportResponse.from(report, true));
    }

    @Transactional(readOnly = true)
    public ReportResponse getReportForTherapist(UUID userId, UUID childId, UUID reportId) {
        User user = loadUser(userId);
        loadChildWithAnyPermissionOrAdmin(
                user,
                childId,
                ChildPermissionType.VIEW_REPORT,
                ChildPermissionType.ASSIGN_MISSION,
                ChildPermissionType.MANAGE
        );
        return ReportResponse.from(loadReport(childId, reportId), true);
    }

    @Transactional
    public ReportResponse updateReport(UUID userId, UUID childId, UUID reportId, ReportUpdateRequest request) {
        User user = loadUser(userId);
        loadChildWithAnyPermissionOrAdmin(
                user,
                childId,
                ChildPermissionType.ASSIGN_MISSION,
                ChildPermissionType.MANAGE
        );
        AiReport report = loadReport(childId, reportId);
        report.updateDraft(request.title().trim(), request.content().trim());
        return ReportResponse.from(report, true);
    }

    @Transactional
    public ReportResponse reviewReport(UUID userId, UUID childId, UUID reportId, ReportUpdateRequest request) {
        User user = loadUser(userId);
        loadChildWithAnyPermissionOrAdmin(
                user,
                childId,
                ChildPermissionType.ASSIGN_MISSION,
                ChildPermissionType.MANAGE
        );
        AiReport report = loadReport(childId, reportId);
        report.review(user, request.title().trim(), request.content().trim());
        return ReportResponse.from(report, true);
    }

    @Transactional
    public ReportResponse publishReport(UUID userId, UUID childId, UUID reportId) {
        User user = loadUser(userId);
        loadChildWithAnyPermissionOrAdmin(
                user,
                childId,
                ChildPermissionType.ASSIGN_MISSION,
                ChildPermissionType.MANAGE
        );
        AiReport report = loadReport(childId, reportId);
        report.publish(user);
        return ReportResponse.from(report, true);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> getPublishedReportsForParent(UUID userId, UUID childId, Pageable pageable) {
        User user = loadUser(userId);
        loadChildWithAnyPermissionOrAdmin(user, childId, ChildPermissionType.VIEW_REPORT, ChildPermissionType.MANAGE);
        return aiReportRepository
                .findByChild_ChildIdAndStatusOrderByCreatedAtDesc(childId, ReportStatus.PUBLISHED, pageable)
                .map(report -> ReportResponse.from(report, false));
    }

    @Transactional(readOnly = true)
    public ReportResponse getPublishedReportForParent(UUID userId, UUID childId, UUID reportId) {
        User user = loadUser(userId);
        loadChildWithAnyPermissionOrAdmin(user, childId, ChildPermissionType.VIEW_REPORT, ChildPermissionType.MANAGE);
        AiReport report = aiReportRepository.findByReportIdAndChild_ChildIdAndStatus(
                        reportId,
                        childId,
                        ReportStatus.PUBLISHED
                )
                .orElseThrow(() -> new EntityNotFoundException("Published report not found."));
        return ReportResponse.from(report, false);
    }

    private User loadUser(UUID userId) {
        return userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found."));
    }

    private AiReport loadReport(UUID childId, UUID reportId) {
        return aiReportRepository.findByReportIdAndChild_ChildId(reportId, childId)
                .orElseThrow(() -> new EntityNotFoundException("Report not found."));
    }

    private Child loadChildWithAnyPermissionOrAdmin(
            User user,
            UUID childId,
            ChildPermissionType... permissions
    ) {
        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new EntityNotFoundException("Child not found."));
        if (user.isAdmin()) {
            return child;
        }
        for (ChildPermissionType permission : permissions) {
            if (child.hasPermission(user.getUserId(), permission)) {
                return child;
            }
        }
        throw new AuthenticationFailedException("You do not have permission to access this child.");
    }

    private String resolveTitle(String title, String fallback) {
        if (title == null || title.isBlank()) {
            return fallback + " report draft";
        }
        return title.trim();
    }
}
