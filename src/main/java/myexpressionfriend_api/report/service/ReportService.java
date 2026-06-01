package myexpressionfriend_api.report.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.auth.domain.user.User;
import myexpressionfriend_api.auth.repository.UserRepository;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.domain.ChildPermissionType;
import myexpressionfriend_api.child.domain.ChildrenAuthorizedUser;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.child.repository.ChildrenAuthorizedUserRepository;
import myexpressionfriend_api.common.exception.AuthenticationFailedException;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import myexpressionfriend_api.report.domain.AiReport;
import myexpressionfriend_api.report.domain.ReportStatus;
import myexpressionfriend_api.report.domain.ReportType;
import myexpressionfriend_api.report.dto.ReportGenerateDraftRequest;
import myexpressionfriend_api.report.dto.ReportResponse;
import myexpressionfriend_api.report.dto.ReportUpdateRequest;
import myexpressionfriend_api.report.event.ReportPublishedEvent;
import myexpressionfriend_api.report.repository.AiReportRepository;
import myexpressionfriend_api.rag.dto.RagGenerateRequest;
import myexpressionfriend_api.rag.dto.RagGenerateResponse;
import myexpressionfriend_api.rag.service.RagGenerationService;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ChildrenAuthorizedUserRepository authorizedUserRepository;
    private final RagGenerationService ragGenerationService;
    private final ChildReportContextBuilder childReportContextBuilder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReportResponse generateDraft(UUID userId, ReportGenerateDraftRequest request) {
        User user = loadUser(userId);
        Child child = loadChildWithAnyPermissionOrAdmin(
                user,
                request.childId(),
                ChildPermissionType.ASSIGN_MISSION,
                ChildPermissionType.MANAGE
        );

        String childSummary = (request.childSummary() != null && !request.childSummary().isBlank())
                ? request.childSummary()
                : childReportContextBuilder.build(child.getChildId());

        String templateKey = resolveTemplateKey(request.templateKey(), request.reportType());

        RagGenerateResponse generated = ragGenerationService.generateReport(new RagGenerateRequest(
                child.getChildId(),
                request.request(),
                request.retrievalQuery(),
                childSummary,
                request.additionalContext(),
                templateKey,
                request.topK(),
                request.similarityThreshold(),
                request.useProModel(),
                true,
                null
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
                ChildPermissionType.VIEW_REPORT
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
                ChildPermissionType.VIEW_REPORT
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
        Child child = loadChildWithAnyPermissionOrAdmin(
                user,
                childId,
                ChildPermissionType.ASSIGN_MISSION,
                ChildPermissionType.MANAGE
        );
        AiReport report = loadReport(childId, reportId);
        report.publish(user);
        // VIEW_REPORT 권한을 가진 모든 보호자에게 각각 알림 이벤트를 발행한다.
        authorizedUserRepository
                .findByChildIdAndPermission(childId, ChildPermissionType.VIEW_REPORT)
                .stream()
                .map(ChildrenAuthorizedUser::getUser)
                .map(User::getUserId)
                .forEach(parentId -> eventPublisher.publishEvent(
                        new ReportPublishedEvent(report.getReportId(), childId, child.getName(), parentId)));
        return ReportResponse.from(report, true);
    }

    @Transactional
    public ReportResponse archiveReport(UUID userId, UUID childId, UUID reportId) {
        User user = loadUser(userId);
        loadChildWithAnyPermissionOrAdmin(
                user,
                childId,
                ChildPermissionType.ASSIGN_MISSION,
                ChildPermissionType.MANAGE
        );
        AiReport report = loadReport(childId, reportId);
        report.archive();
        return ReportResponse.from(report, true);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> getPublishedReportsForParent(UUID userId, UUID childId, Pageable pageable) {
        User user = loadUser(userId);
        loadChildWithAnyPermissionOrAdmin(user, childId, ChildPermissionType.VIEW_REPORT);
        return aiReportRepository
                .findByChild_ChildIdAndStatusOrderByCreatedAtDesc(childId, ReportStatus.PUBLISHED, pageable)
                .map(report -> ReportResponse.from(report, false));
    }

    @Transactional(readOnly = true)
    public ReportResponse getPublishedReportForParent(UUID userId, UUID childId, UUID reportId) {
        User user = loadUser(userId);
        loadChildWithAnyPermissionOrAdmin(user, childId, ChildPermissionType.VIEW_REPORT);
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

    private String resolveTemplateKey(String requestedKey, ReportType reportType) {
        if (requestedKey != null && !requestedKey.isBlank()) {
            return requestedKey;
        }
        return switch (reportType) {
            case WEEKLY -> "report-generation-weekly";
            case MONTHLY -> "report-generation-monthly";
            case CUSTOM -> "report-generation-default";
        };
    }

    private String resolveTitle(String title, String fallback) {
        if (title == null || title.isBlank()) {
            return fallback + " report draft";
        }
        return title.trim();
    }
}
