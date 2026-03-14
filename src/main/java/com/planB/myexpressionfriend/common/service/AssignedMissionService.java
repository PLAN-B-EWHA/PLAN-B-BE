package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.mission.AssignedMission;
import com.planB.myexpressionfriend.common.domain.mission.MissionReviewDecision;
import com.planB.myexpressionfriend.common.domain.mission.MissionStatus;
import com.planB.myexpressionfriend.common.domain.mission.MissionTemplate;
import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.dto.mission.AssignedMissionCreateDTO;
import com.planB.myexpressionfriend.common.dto.mission.MissionBatchReviewRequestDTO;
import com.planB.myexpressionfriend.common.dto.mission.MissionBatchReviewResultDTO;
import com.planB.myexpressionfriend.common.dto.mission.MissionReviewQueueItemDTO;
import com.planB.myexpressionfriend.common.dto.mission.AssignedMissionDTO;
import com.planB.myexpressionfriend.common.dto.mission.AssignedMissionDetailDTO;
import com.planB.myexpressionfriend.common.dto.mission.MissionSearchDTO;
import com.planB.myexpressionfriend.common.dto.mission.MissionStatusUpdateDTO;
import com.planB.myexpressionfriend.common.dto.note.ChildNoteDTO;
import com.planB.myexpressionfriend.common.dto.note.PageResponseDTO;
import com.planB.myexpressionfriend.common.event.MissionCompletedEvent;
import com.planB.myexpressionfriend.common.repository.AssignedMissionRepository;
import com.planB.myexpressionfriend.common.repository.ChildNoteRepository;
import com.planB.myexpressionfriend.common.repository.ChildRepository;
import com.planB.myexpressionfriend.common.repository.MissionTemplateRepository;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import com.planB.myexpressionfriend.common.exception.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AssignedMissionService {

    private final AssignedMissionRepository missionRepository;
    private final MissionTemplateRepository templateRepository;
    private final ChildRepository childRepository;
    private final UserRepository userRepository;
    private final ChildNoteRepository childNoteRepository;
    private final ChildNoteService noteService;
    private final ChildAuthorizationService childAuthorizationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AssignedMissionDTO assignMission(AssignedMissionCreateDTO dto, UUID therapistId) {
        Child child = childRepository.findById(dto.getChildId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 아동입니다."));

        User therapist = userRepository.findById(therapistId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 사용자입니다."));

        MissionTemplate template = templateRepository.findByIdAndActive(dto.getTemplateId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않거나 비활성화된 템플릿입니다."));

        if (!child.hasPermission(therapistId, ChildPermissionType.ASSIGN_MISSION)) {
            throw new AccessDeniedException("해당 아동에 대한 ASSIGN_MISSION 권한이 없습니다.");
        }

        AssignedMission mission = AssignedMission.builder()
                .child(child)
                .therapist(therapist)
                .template(template)
                .status(MissionStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .dueDate(dto.getDueDate())
                .isDeleted(false)
                .build();

        AssignedMission savedMission = missionRepository.save(mission);
        createSystemNoteForAssignment(savedMission, therapist);
        return AssignedMissionDTO.from(savedMission);
    }

    public AssignedMissionDetailDTO getMission(UUID missionId, UUID userId) {
        AssignedMission mission = missionRepository.findByIdWithAuth(missionId, userId)
                .orElseThrow(() -> new AccessDeniedException("미션 조회 권한이 없거나 존재하지 않는 미션입니다."));
        return AssignedMissionDetailDTO.from(mission);
    }

    public PageResponseDTO<AssignedMissionDTO> getMissionsByChild(UUID childId, UUID userId, Pageable pageable) {
        assertViewReportPermission(childId, userId);
        Page<AssignedMission> page = missionRepository.findByChildIdWithAuth(childId, userId, pageable);
        return PageResponseDTO.from(page, AssignedMissionDTO::from);
    }

    public PageResponseDTO<AssignedMissionDTO> searchMissions(MissionSearchDTO searchDTO, UUID userId) {
        searchDTO.validate();
        assertViewReportPermission(searchDTO.getChildId(), userId);
        Pageable pageable = searchDTO.toPageable();

        Page<AssignedMission> page;
        if (searchDTO.getStatus() != null) {
            page = missionRepository.findByChildIdAndStatusWithAuth(
                    searchDTO.getChildId(), userId, searchDTO.getStatus(), pageable
            );
        } else if (searchDTO.getTherapistId() != null) {
            page = missionRepository.findByChildIdAndTherapistWithAuth(
                    searchDTO.getChildId(), userId, searchDTO.getTherapistId(), pageable
            );
        } else if (searchDTO.getStartDate() != null && searchDTO.getEndDate() != null) {
            page = missionRepository.findByChildIdAndDateRangeWithAuth(
                    searchDTO.getChildId(), userId, searchDTO.getStartDate(), searchDTO.getEndDate(), pageable
            );
        } else {
            page = missionRepository.findByChildIdWithAuth(searchDTO.getChildId(), userId, pageable);
        }

        return PageResponseDTO.from(page, AssignedMissionDTO::from);
    }

    public PageResponseDTO<AssignedMissionDTO> getOverdueMissions(UUID childId, UUID userId, Pageable pageable) {
        assertViewReportPermission(childId, userId);
        Page<AssignedMission> page = missionRepository.findOverdueMissionsWithAuth(
                childId, userId, LocalDateTime.now(), pageable
        );
        return PageResponseDTO.from(page, AssignedMissionDTO::from);
    }

    public PageResponseDTO<AssignedMissionDetailDTO> getPendingVerificationMissions(
            UUID childId,
            UUID userId,
            Pageable pageable
    ) {
        assertViewReportPermission(childId, userId);
        Page<AssignedMission> page = missionRepository.findPendingVerificationWithAuth(childId, userId, pageable);
        return PageResponseDTO.from(page, AssignedMissionDetailDTO::from);
    }

    public PageResponseDTO<MissionReviewQueueItemDTO> getReviewQueue(
            UUID therapistId,
            UUID childId,
            Pageable pageable
    ) {
        Page<AssignedMission> page = missionRepository.findReviewQueueByTherapist(therapistId, childId, pageable);
        return PageResponseDTO.from(page, MissionReviewQueueItemDTO::from);
    }

    @Transactional
    public MissionBatchReviewResultDTO batchReviewMissions(UUID therapistId, MissionBatchReviewRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.getMissionIds() == null || requestDTO.getMissionIds().isEmpty()) {
            return MissionBatchReviewResultDTO.empty();
        }

        if (requestDTO.getReviewDecision() == MissionReviewDecision.REJECT
                && (requestDTO.getTherapistFeedback() == null || requestDTO.getTherapistFeedback().isBlank())) {
            throw new IllegalArgumentException("반려 사유(치료사 피드백)를 입력해주세요.");
        }

        List<UUID> requested = new ArrayList<>(new LinkedHashSet<>(requestDTO.getMissionIds()));
        List<UUID> succeeded = new ArrayList<>();
        List<MissionBatchReviewResultDTO.FailureItem> failures = new ArrayList<>();

        for (UUID missionId : requested) {
            try {
                MissionStatusUpdateDTO updateDTO = MissionStatusUpdateDTO.builder()
                        .reviewDecision(requestDTO.getReviewDecision())
                        .therapistFeedback(requestDTO.getTherapistFeedback())
                        .build();
                reviewMission(missionId, updateDTO, therapistId);
                succeeded.add(missionId);
            } catch (Exception e) {
                failures.add(MissionBatchReviewResultDTO.FailureItem.builder()
                        .missionId(missionId)
                        .reason(e.getMessage())
                        .build());
            }
        }

        return MissionBatchReviewResultDTO.builder()
                .requestedCount(requested.size())
                .successCount(succeeded.size())
                .failureCount(failures.size())
                .succeededMissionIds(succeeded)
                .failures(failures)
                .build();
    }

    @Transactional
    public AssignedMissionDTO startMission(UUID missionId, UUID userId) {
        AssignedMission mission = missionRepository.findByIdWithAuth(missionId, userId)
                .orElseThrow(() -> new AccessDeniedException("미션 조회 권한이 없거나 존재하지 않는 미션입니다."));

        if (!mission.canStart(userId)) {
            throw new AccessDeniedException("미션 시작 권한이 없습니다.");
        }

        mission.start();
        return AssignedMissionDTO.from(mission);
    }

    @Transactional
    public AssignedMissionDTO completeMission(UUID missionId, MissionStatusUpdateDTO dto, UUID userId) {
        AssignedMission mission = missionRepository.findByIdWithAuth(missionId, userId)
                .orElseThrow(() -> new AccessDeniedException("미션 조회 권한이 없거나 존재하지 않는 미션입니다."));

        if (!mission.canComplete(userId)) {
            throw new AccessDeniedException("미션 완료 권한이 없습니다.");
        }

        mission.complete(dto.getParentNote());
        User parent = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 사용자입니다."));
        createSystemNoteForCompletion(mission, parent);

        eventPublisher.publishEvent(new MissionCompletedEvent(
                mission.getMissionId(),
                mission.getTherapist().getUserId(),
                parent.getUserId()
        ));

        return AssignedMissionDTO.from(mission);
    }

    @Transactional
    public AssignedMissionDTO verifyMission(UUID missionId, MissionStatusUpdateDTO dto, UUID userId) {
        return reviewMission(missionId, dto, userId);
    }

    @Transactional
    public AssignedMissionDTO reviewMission(UUID missionId, MissionStatusUpdateDTO dto, UUID userId) {
        AssignedMission mission = missionRepository.findByIdWithAuth(missionId, userId)
                .orElseThrow(() -> new AccessDeniedException("미션 조회 권한이 없거나 존재하지 않는 미션입니다."));

        if (!mission.canVerify(userId)) {
            throw new AccessDeniedException("미션 검증 권한이 없습니다.");
        }

        MissionReviewDecision decision = dto.getReviewDecision() == null
                ? MissionReviewDecision.APPROVE
                : dto.getReviewDecision();

        if (decision == MissionReviewDecision.REJECT
                && (dto.getTherapistFeedback() == null || dto.getTherapistFeedback().isBlank())) {
            throw new IllegalArgumentException("반려 사유(치료사 피드백)를 입력해주세요.");
        }

        User therapist = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 사용자입니다."));

        if (decision == MissionReviewDecision.REJECT) {
            mission.reject(dto.getTherapistFeedback());
            createSystemNoteForRejection(mission, therapist);
        } else {
            mission.verify(dto.getTherapistFeedback());
            createSystemNoteForVerification(mission, therapist);
        }

        return AssignedMissionDTO.from(mission);
    }

    @Transactional
    public AssignedMissionDTO updateMissionStatus(UUID missionId, MissionStatusUpdateDTO dto, UUID userId) {
        if (dto.getStatus() == null) {
            throw new IllegalArgumentException("status 값이 필요합니다.");
        }

        return switch (dto.getStatus()) {
            case IN_PROGRESS -> startMission(missionId, userId);
            case COMPLETED -> completeMission(missionId, dto, userId);
            case VERIFIED -> verifyMission(missionId, dto, userId);
            case CANCELLED -> {
                cancelMission(missionId, userId);
                AssignedMission mission = missionRepository.findByIdWithAuth(missionId, userId)
                        .orElseThrow(() -> new AccessDeniedException("미션 조회 권한이 없거나 존재하지 않는 미션입니다."));
                yield AssignedMissionDTO.from(mission);
            }
            case ASSIGNED -> throw new IllegalArgumentException("ASSIGNED 상태로는 변경할 수 없습니다.");
        };
    }

    @Transactional
    public void cancelMission(UUID missionId, UUID userId) {
        AssignedMission mission = missionRepository.findByIdWithAuth(missionId, userId)
                .orElseThrow(() -> new AccessDeniedException("미션 조회 권한이 없거나 존재하지 않는 미션입니다."));

        if (!mission.canCancel(userId)) {
            throw new AccessDeniedException("미션 취소 권한이 없습니다.");
        }

        mission.cancel();
    }

    @Transactional
    public void deleteMission(UUID missionId, UUID userId) {
        AssignedMission mission = missionRepository.findByIdWithAuth(missionId, userId)
                .orElseThrow(() -> new AccessDeniedException("미션 조회 권한이 없거나 존재하지 않는 미션입니다."));

        if (!mission.isTherapist(userId)) {
            throw new AccessDeniedException("미션 삭제 권한이 없습니다.");
        }

        mission.delete(userId);
    }

    public long countMissionsByChild(UUID childId, UUID userId) {
        assertViewReportPermission(childId, userId);
        return missionRepository.countByChildIdWithAuth(childId, userId);
    }

    public long countMissionsByChildAndStatus(UUID childId, UUID userId, MissionStatus status) {
        assertViewReportPermission(childId, userId);
        return missionRepository.countByChildIdAndStatusWithAuth(childId, userId, status);
    }

    private void assertViewReportPermission(UUID childId, UUID userId) {
        boolean hasPermission = childAuthorizationService.hasPermission(childId, userId, ChildPermissionType.VIEW_REPORT);
        if (!hasPermission) {
            throw new AccessDeniedException("해당 아동 VIEW_REPORT 권한이 없습니다.");
        }
    }

    private void createSystemNoteForAssignment(AssignedMission mission, User therapist) {
        String title = String.format("미션 할당: %s", mission.getTemplate().getTitle());
        String content = String.format(
                """
                **미션이 할당되었습니다.**

                - 미션: %s
                - 카테고리: %s
                - 난이도: %s
                - 할당 치료사: %s
                - 목표 완료일: %s

                %s
                """,
                mission.getTemplate().getTitle(),
                mission.getTemplate().getCategory().getDisplayName(),
                mission.getTemplate().getDifficulty().getDisplayName(),
                therapist.getName(),
                mission.getDueDate() != null ? mission.getDueDate().toString() : "없음",
                mission.getTemplate().getInstructions()
        );
        linkCreatedSystemNote(mission, noteService.createSystemNote(
                mission.getChild().getChildId(),
                therapist.getUserId(),
                title,
                content
        ));
    }

    private void createSystemNoteForCompletion(AssignedMission mission, User parent) {
        String title = String.format("미션 완료: %s", mission.getTemplate().getTitle());
        String content = String.format(
                """
                **미션이 완료되었습니다.**

                - 미션: %s
                - 완료 시각: %s
                - 완료자: %s
                - 부모 노트: %s
                - 사진 개수: %d
                """,
                mission.getTemplate().getTitle(),
                mission.getCompletedAt(),
                parent.getName(),
                mission.getParentNote() != null ? mission.getParentNote() : "없음",
                mission.getPhotos().size()
        );
        linkCreatedSystemNote(mission, noteService.createSystemNote(
                mission.getChild().getChildId(),
                parent.getUserId(),
                title,
                content
        ));
    }

    private void createSystemNoteForVerification(AssignedMission mission, User therapist) {
        String title = String.format("미션 검증 완료: %s", mission.getTemplate().getTitle());
        String content = String.format(
                """
                **미션 검증이 완료되었습니다.**

                - 미션: %s
                - 검증 시각: %s
                - 검증 치료사: %s
                - 피드백: %s
                """,
                mission.getTemplate().getTitle(),
                mission.getVerifiedAt(),
                therapist.getName(),
                mission.getTherapistFeedback() != null ? mission.getTherapistFeedback() : "없음"
        );
        linkCreatedSystemNote(mission, noteService.createSystemNote(
                mission.getChild().getChildId(),
                therapist.getUserId(),
                title,
                content
        ));
    }

    private void createSystemNoteForRejection(AssignedMission mission, User therapist) {
        String title = String.format("미션 반려: %s", mission.getTemplate().getTitle());
        String content = String.format(
                """
                **미션이 반려되었습니다.**

                - 미션: %s
                - 반려 시각: %s
                - 반려 치료사: %s
                - 반려 사유: %s
                """,
                mission.getTemplate().getTitle(),
                LocalDateTime.now(),
                therapist.getName(),
                mission.getTherapistFeedback() != null ? mission.getTherapistFeedback() : "없음"
        );
        linkCreatedSystemNote(mission, noteService.createSystemNote(
                mission.getChild().getChildId(),
                therapist.getUserId(),
                title,
                content
        ));
    }

    private void linkCreatedSystemNote(AssignedMission mission, ChildNoteDTO noteDTO) {
        if (noteDTO == null || noteDTO.getNoteId() == null) {
            return;
        }
        ChildNote note = childNoteRepository.findById(noteDTO.getNoteId()).orElse(null);
        if (note != null) {
            mission.linkSystemNote(note);
        }
    }
}




