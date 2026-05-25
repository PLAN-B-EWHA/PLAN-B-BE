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
import myexpressionfriend_api.homework.dto.HomeworkMissionSummaryResponse;
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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public HomeworkAssignmentResponse getCurrentAssignment(UUID userId, UUID childId) {
        loadChildWithAnyPermission(userId, childId, ChildPermissionType.VIEW_REPORT, ChildPermissionType.ASSIGN_MISSION);
        return homeworkAssignmentRepository
                .findByChild_ChildIdAndStatusOrderByDueDateAscCreatedAtDesc(childId, HomeworkStatus.PENDING)
                .stream()
                .min(Comparator
                        .comparing((HomeworkAssignment h) -> h.getDueDate() == null ? LocalDate.MAX : h.getDueDate())
                        .thenComparing(HomeworkAssignment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public HomeworkMissionSummaryResponse getMissionSummary(UUID userId, UUID childId) {
        loadChildWithAnyPermission(userId, childId, ChildPermissionType.VIEW_REPORT, ChildPermissionType.ASSIGN_MISSION);
        List<HomeworkAssignment> assignments = homeworkAssignmentRepository.findByChild_ChildId(childId);
        List<HomeworkReport> reports = homeworkReportRepository.findByHomework_Child_ChildId(childId);
        Map<UUID, HomeworkReport> reportByHomeworkId = reports.stream()
                .collect(Collectors.toMap(
                        report -> report.getHomework().getHomeworkId(),
                        report -> report,
                        (first, ignored) -> first));

        SummaryCounter total = new SummaryCounter();
        Map<Integer, SummaryCounter> byWeek = new java.util.TreeMap<>();
        Map<Integer, StrategyFocus> strategyByWeek = new java.util.HashMap<>();
        LocalDate today = LocalDate.now();

        for (HomeworkAssignment assignment : assignments) {
            HomeworkReport report = reportByHomeworkId.get(assignment.getHomeworkId());
            total.accept(assignment, report, today);
            byWeek.computeIfAbsent(assignment.getWeek(), ignored -> new SummaryCounter())
                    .accept(assignment, report, today);
            strategyByWeek.putIfAbsent(assignment.getWeek(), assignment.getStrategyFocus());
        }

        List<HomeworkMissionSummaryResponse.WeekSummary> weeks = byWeek.entrySet().stream()
                .map(entry -> {
                    int week = entry.getKey();
                    SummaryCounter counter = entry.getValue();
                    StrategyFocus strategy = strategyByWeek.get(week);
                    return new HomeworkMissionSummaryResponse.WeekSummary(
                            week,
                            strategy == null ? null : strategy.name(),
                            strategyFocusLabel(strategy),
                            counter.assignedCount,
                            counter.submittedCount,
                            counter.doneCount,
                            counter.partialCount,
                            counter.notDoneCount,
                            ratio(counter.submittedCount, counter.assignedCount),
                            ratio(counter.doneCount + counter.partialCount, counter.assignedCount),
                            ratio(counter.doneCount, counter.submittedCount),
                            ratio(counter.spontaneousCount, counter.submittedCount)
                    );
                })
                .toList();

        return new HomeworkMissionSummaryResponse(
                childId,
                total.assignedCount,
                total.statusCounts.getOrDefault(HomeworkStatus.PENDING, 0),
                total.statusCounts.getOrDefault(HomeworkStatus.SUBMITTED, 0),
                total.statusCounts.getOrDefault(HomeworkStatus.REVIEWED, 0),
                total.statusCounts.getOrDefault(HomeworkStatus.CANCELED, 0),
                total.overduePendingCount,
                total.dueSoonPendingCount,
                total.doneCount,
                total.partialCount,
                total.notDoneCount,
                total.spontaneousCount,
                ratio(total.submittedCount, total.assignedCount),
                ratio(total.doneCount + total.partialCount, total.assignedCount),
                ratio(total.doneCount, total.submittedCount),
                ratio(total.spontaneousCount, total.submittedCount),
                weeks
        );
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
    public HomeworkAssignmentResponse updateReport(
            UUID userId,
            UUID childId,
            UUID homeworkId,
            HomeworkReportSubmitRequest request
    ) {
        loadChildWithAnyPermission(userId, childId, ChildPermissionType.VIEW_REPORT, ChildPermissionType.MANAGE);
        HomeworkAssignment homework = loadHomework(childId, homeworkId);
        if (homework.getStatus() != HomeworkStatus.SUBMITTED) {
            throw new InvalidRequestException("검토 전 제출 기록만 수정할 수 있습니다. status=" + homework.getStatus());
        }
        HomeworkReport report = homeworkReportRepository.findByHomework_HomeworkId(homeworkId)
                .orElseThrow(() -> new InvalidRequestException("수정할 제출 기록이 없습니다."));

        report.updateSubmission(
                request.completed(),
                request.initiatedBy(),
                request.strategyApplied() == null ? homework.getStrategyFocus() : request.strategyApplied(),
                trimToNull(request.parentObservation()),
                trimToNull(request.peerResponseObserved()),
                request.spontaneousFlag()
        );
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

    private double ratio(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return (double) numerator / denominator;
    }

    private String strategyFocusLabel(StrategyFocus strategyFocus) {
        if (strategyFocus == null) return null;
        return switch (strategyFocus) {
            case INFORMATION_EXCHANGE -> "정보 교환하기";
            case CONVERSATION_MAINTENANCE -> "대화 유지하기";
            case FINDING_COMMON_GROUND -> "공통점 찾기";
            case CONVERSATION_INITIATION -> "대화 시작하기";
            case CONVERSATION_EXIT -> "대화 마무리하기";
            case DIGITAL_COMMUNICATION -> "전자 의사소통";
            case FRIEND_SELECTION -> "친구 선택하기";
            case HUMOR_USE -> "유머 사용하기";
            case GOOD_SPORTSMANSHIP -> "좋은 스포츠맨십";
            case PLAYING_TOGETHER -> "함께 놀기";
            case CONFLICT_RESOLUTION -> "갈등 해결하기";
            case HANDLING_TEASING -> "놀림에 대처하기";
            case HANDLING_EXCLUSION -> "소외에 대처하기";
            case HANDLING_CYBERBULLYING -> "사이버 괴롭힘 대처하기";
            case HANDLING_RUMORS -> "소문과 험담 대처하기";
            case REPUTATION_MANAGEMENT -> "평판 관리하기";
        };
    }

    private static class SummaryCounter {
        private int assignedCount;
        private int submittedCount;
        private int overduePendingCount;
        private int dueSoonPendingCount;
        private int doneCount;
        private int partialCount;
        private int notDoneCount;
        private int spontaneousCount;
        private final Map<HomeworkStatus, Integer> statusCounts = new EnumMap<>(HomeworkStatus.class);

        private void accept(HomeworkAssignment assignment, HomeworkReport report, LocalDate today) {
            assignedCount++;
            statusCounts.merge(assignment.getStatus(), 1, Integer::sum);
            if (assignment.getStatus() == HomeworkStatus.PENDING && assignment.getDueDate() != null) {
                if (assignment.getDueDate().isBefore(today)) {
                    overduePendingCount++;
                } else if (!assignment.getDueDate().isAfter(today.plusDays(2))) {
                    dueSoonPendingCount++;
                }
            }
            if (report == null) {
                return;
            }
            submittedCount++;
            if (Boolean.TRUE.equals(report.getSpontaneousFlag())) {
                spontaneousCount++;
            }
            switch (report.getCompleted()) {
                case DONE -> doneCount++;
                case PARTIAL -> partialCount++;
                case NOT_DONE -> notDoneCount++;
            }
        }
    }
}
