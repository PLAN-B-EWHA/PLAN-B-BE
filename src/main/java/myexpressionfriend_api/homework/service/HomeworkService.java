package myexpressionfriend_api.homework.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.auth.domain.user.User;
import myexpressionfriend_api.auth.repository.UserRepository;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.domain.ChildPermissionType;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.common.exception.AuthenticationFailedException;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import myexpressionfriend_api.common.exception.InvalidRequestException;
import myexpressionfriend_api.homework.domain.HomeworkAssignment;
import myexpressionfriend_api.homework.domain.HomeworkReport;
import myexpressionfriend_api.homework.domain.HomeworkStatus;
import myexpressionfriend_api.homework.domain.StrategyFocus;
import myexpressionfriend_api.homework.domain.StrategyTipSource;
import myexpressionfriend_api.homework.dto.HomeworkAssignmentCreateRequest;
import myexpressionfriend_api.homework.dto.HomeworkAssignmentResponse;
import myexpressionfriend_api.homework.dto.HomeworkAssignmentUpdateRequest;
import myexpressionfriend_api.homework.dto.HomeworkGenerateMissionRequest;
import myexpressionfriend_api.homework.dto.HomeworkReportResponse;
import myexpressionfriend_api.homework.dto.HomeworkReportSubmitRequest;
import myexpressionfriend_api.homework.dto.HomeworkReviewRequest;
import myexpressionfriend_api.homework.repository.HomeworkAssignmentRepository;
import myexpressionfriend_api.homework.repository.HomeworkReportRepository;
import myexpressionfriend_api.rag.dto.RagGenerateRequest;
import myexpressionfriend_api.rag.dto.RagGenerateResponse;
import myexpressionfriend_api.rag.service.RagGenerationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HomeworkService {

    private final ChildRepository childRepository;
    private final UserRepository userRepository;
    private final HomeworkAssignmentRepository homeworkAssignmentRepository;
    private final HomeworkReportRepository homeworkReportRepository;
    private final RagGenerationService ragGenerationService;

    @Transactional(readOnly = true)
    public Page<HomeworkAssignmentResponse> getAssignments(
            UUID userId,
            UUID childId,
            HomeworkStatus status,
            Pageable pageable
    ) {
        loadChildWithAnyPermission(userId, childId, ChildPermissionType.VIEW_REPORT, ChildPermissionType.ASSIGN_MISSION);

        Page<HomeworkAssignment> assignments = status == null
                ? homeworkAssignmentRepository.findByChild_ChildIdOrderByCreatedAtDesc(childId, pageable)
                : homeworkAssignmentRepository.findByChild_ChildIdAndStatusOrderByCreatedAtDesc(childId, status, pageable);

        return assignments.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public HomeworkAssignmentResponse getAssignment(UUID userId, UUID childId, UUID homeworkId) {
        loadChildWithAnyPermission(userId, childId, ChildPermissionType.VIEW_REPORT, ChildPermissionType.ASSIGN_MISSION);
        return toResponse(loadHomework(childId, homeworkId));
    }

    @Transactional
    public HomeworkAssignmentResponse createAssignment(
            UUID userId,
            UUID childId,
            HomeworkAssignmentCreateRequest request
    ) {
        Child child = loadChildWithAnyPermission(userId, childId, ChildPermissionType.ASSIGN_MISSION, ChildPermissionType.MANAGE);

        int week = resolveWeek(request.week(), request.strategyFocus());
        StrategyFocus strategyFocus = request.strategyFocus() == null
                ? StrategyFocus.ofWeek(week)
                : request.strategyFocus();

        HomeworkAssignment homework = HomeworkAssignment.builder()
                .child(child)
                .week(week)
                .strategyFocus(strategyFocus)
                .instruction(trimToNull(request.instruction()))
                .strategyTip(trimToNull(request.strategyTip()))
                .strategyTipSource(request.strategyTipSource() == null ? StrategyTipSource.MANUAL : request.strategyTipSource())
                .dueDate(request.dueDate() == null ? LocalDate.now().plusDays(7) : request.dueDate())
                .status(HomeworkStatus.PENDING)
                .build();

        return toResponse(homeworkAssignmentRepository.save(homework));
    }

    @Transactional
    public HomeworkAssignmentResponse generateOfflineMission(
            UUID userId,
            UUID childId,
            HomeworkGenerateMissionRequest request
    ) {
        Child child = loadChildWithAnyPermission(userId, childId, ChildPermissionType.ASSIGN_MISSION, ChildPermissionType.MANAGE);

        int week = resolveWeek(request.week(), request.strategyFocus());
        StrategyFocus strategyFocus = request.strategyFocus() == null
                ? StrategyFocus.ofWeek(week)
                : request.strategyFocus();

        RagGenerateResponse generated = ragGenerationService.generateOfflineMission(new RagGenerateRequest(
                childId,
                request.request(),
                request.retrievalQuery(),
                request.childSummary(),
                request.additionalContext(),
                request.templateKey(),
                request.topK(),
                request.similarityThreshold(),
                request.useProModel(),
                false
        ));

        HomeworkAssignment homework = HomeworkAssignment.builder()
                .child(child)
                .week(week)
                .strategyFocus(strategyFocus)
                .instruction(trimToNull(request.request()))
                .strategyTip(generated.generatedText())
                .strategyTipSource(StrategyTipSource.LLM_FLASH)
                .dueDate(request.dueDate() == null ? LocalDate.now().plusDays(7) : request.dueDate())
                .status(HomeworkStatus.PENDING)
                .build();

        return toResponse(homeworkAssignmentRepository.save(homework));
    }

    @Transactional
    public HomeworkAssignmentResponse updateAssignment(
            UUID userId,
            UUID childId,
            UUID homeworkId,
            HomeworkAssignmentUpdateRequest request
    ) {
        loadChildWithAnyPermission(userId, childId, ChildPermissionType.ASSIGN_MISSION, ChildPermissionType.MANAGE);
        HomeworkAssignment homework = loadHomework(childId, homeworkId);

        int week = request.week() == null && request.strategyFocus() == null
                ? homework.getWeek()
                : resolveWeek(request.week(), request.strategyFocus());
        StrategyFocus strategyFocus = request.strategyFocus() == null
                ? StrategyFocus.ofWeek(week)
                : request.strategyFocus();

        homework.updateAssignment(
                week,
                strategyFocus,
                request.instruction() == null ? homework.getInstruction() : trimToNull(request.instruction()),
                request.strategyTip() == null ? homework.getStrategyTip() : trimToNull(request.strategyTip()),
                request.strategyTipSource() == null ? homework.getStrategyTipSource() : request.strategyTipSource(),
                request.dueDate() == null ? homework.getDueDate() : request.dueDate()
        );

        return toResponse(homework);
    }

    @Transactional
    public HomeworkAssignmentResponse cancelAssignment(UUID userId, UUID childId, UUID homeworkId) {
        loadChildWithAnyPermission(userId, childId, ChildPermissionType.ASSIGN_MISSION, ChildPermissionType.MANAGE);
        HomeworkAssignment homework = loadHomework(childId, homeworkId);
        homework.cancel();
        return toResponse(homework);
    }

    @Transactional
    public HomeworkAssignmentResponse submitReport(
            UUID userId,
            UUID childId,
            UUID homeworkId,
            HomeworkReportSubmitRequest request
    ) {
        loadChildWithAnyPermission(userId, childId, ChildPermissionType.VIEW_REPORT, ChildPermissionType.MANAGE);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found."));
        HomeworkAssignment homework = loadHomework(childId, homeworkId);

        if (homeworkReportRepository.existsByHomework_HomeworkId(homeworkId)) {
            throw new InvalidRequestException("이미 제출된 숙제 리포트가 있습니다.");
        }
        if (homework.getStatus() == HomeworkStatus.PENDING) {
            homework.submit();
        } else if (homework.getStatus() != HomeworkStatus.SUBMITTED) {
            throw new InvalidRequestException("제출할 수 없는 숙제 상태입니다. status=" + homework.getStatus());
        }

        HomeworkReport report = HomeworkReport.builder()
                .homework(homework)
                .reportedBy(user)
                .completed(request.completed())
                .initiatedBy(request.initiatedBy())
                .strategyApplied(request.strategyApplied() == null ? homework.getStrategyFocus() : request.strategyApplied())
                .parentObservation(trimToNull(request.parentObservation()))
                .peerResponseObserved(trimToNull(request.peerResponseObserved()))
                .spontaneousFlag(Boolean.TRUE.equals(request.spontaneousFlag()))
                .build();
        homeworkReportRepository.save(report);

        return toResponse(homework);
    }

    @Transactional
    public HomeworkAssignmentResponse review(
            UUID userId,
            UUID childId,
            UUID homeworkId,
            HomeworkReviewRequest request
    ) {
        loadChildWithAnyPermission(userId, childId, ChildPermissionType.ASSIGN_MISSION, ChildPermissionType.MANAGE);
        User reviewer = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found."));
        HomeworkAssignment homework = loadHomework(childId, homeworkId);
        HomeworkReport report = homeworkReportRepository.findByHomework_HomeworkId(homeworkId)
                .orElseThrow(() -> new InvalidRequestException("Submitted homework report is required before review."));
        homework.review();
        report.review(reviewer, request == null ? null : trimToNull(request.reviewComment()));
        return toResponse(homework);
    }

    private HomeworkAssignmentResponse toResponse(HomeworkAssignment homework) {
        HomeworkReportResponse report = homeworkReportRepository.findByHomework_HomeworkId(homework.getHomeworkId())
                .map(HomeworkReportResponse::from)
                .orElse(null);
        return HomeworkAssignmentResponse.from(homework, report);
    }

    private HomeworkAssignment loadHomework(UUID childId, UUID homeworkId) {
        return homeworkAssignmentRepository.findByHomeworkIdAndChild_ChildId(homeworkId, childId)
                .orElseThrow(() -> new EntityNotFoundException("숙제를 찾을 수 없습니다."));
    }

    private Child loadChildWithAnyPermission(UUID userId, UUID childId, ChildPermissionType... permissions) {
        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동 정보를 찾을 수 없습니다."));

        for (ChildPermissionType permission : permissions) {
            if (child.hasPermission(userId, permission)) {
                return child;
            }
        }
        throw new AuthenticationFailedException("해당 아동의 숙제에 접근할 권한이 없습니다.");
    }

    private int resolveWeek(Integer week, StrategyFocus strategyFocus) {
        if (week == null && strategyFocus == null) {
            throw new InvalidRequestException("week 또는 strategyFocus 중 하나는 필요합니다.");
        }
        if (week == null) {
            return strategyFocus.getWeek();
        }
        if (week < 1 || week > 16) {
            throw new InvalidRequestException("week는 1~16 사이여야 합니다.");
        }
        if (strategyFocus != null && strategyFocus.getWeek() != week) {
            throw new InvalidRequestException("week와 strategyFocus가 서로 맞지 않습니다.");
        }
        return week;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
