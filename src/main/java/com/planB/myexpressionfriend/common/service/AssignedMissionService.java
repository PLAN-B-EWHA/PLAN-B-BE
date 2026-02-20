package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.mission.AssignedMission;
import com.planB.myexpressionfriend.common.domain.mission.MissionStatus;
import com.planB.myexpressionfriend.common.domain.mission.MissionTemplate;
import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.dto.mission.AssignedMissionCreateDTO;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AssignedMissionDTO assignMission(AssignedMissionCreateDTO dto, UUID therapistId) {
        Child child = childRepository.findById(dto.getChildId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아동입니다."));

        User therapist = userRepository.findById(therapistId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        MissionTemplate template = templateRepository.findByIdAndActive(dto.getTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 비활성화된 템플릿입니다."));

        if (!child.canAccess(therapistId)) {
            throw new AccessDeniedException("해당 아동에 대한 접근 권한이 없습니다.");
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
        Page<AssignedMission> page = missionRepository.findByChildIdWithAuth(childId, userId, pageable);
        return PageResponseDTO.from(page, AssignedMissionDTO::from);
    }

    public PageResponseDTO<AssignedMissionDTO> searchMissions(MissionSearchDTO searchDTO, UUID userId) {
        searchDTO.validate();
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
        Page<AssignedMission> page = missionRepository.findPendingVerificationWithAuth(childId, userId, pageable);
        return PageResponseDTO.from(page, AssignedMissionDetailDTO::from);
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
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
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
        AssignedMission mission = missionRepository.findByIdWithAuth(missionId, userId)
                .orElseThrow(() -> new AccessDeniedException("미션 조회 권한이 없거나 존재하지 않는 미션입니다."));

        if (!mission.canVerify(userId)) {
            throw new AccessDeniedException("미션 검증 권한이 없습니다.");
        }

        mission.verify(dto.getTherapistFeedback());
        User therapist = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        createSystemNoteForVerification(mission, therapist);
        return AssignedMissionDTO.from(mission);
    }

    @Transactional
    public AssignedMissionDTO updateMissionStatus(UUID missionId, MissionStatusUpdateDTO dto, UUID userId) {
        if (dto.getStatus() == null) {
            throw new IllegalArgumentException("변경할 상태(status)는 필수입니다.");
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
            case ASSIGNED -> throw new IllegalArgumentException("ASSIGNED 상태로 변경은 지원하지 않습니다.");
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

        mission.delete();
    }

    public long countMissionsByChild(UUID childId, UUID userId) {
        return missionRepository.countByChildIdWithAuth(childId, userId);
    }

    public long countMissionsByChildAndStatus(UUID childId, UUID userId, MissionStatus status) {
        return missionRepository.countByChildIdAndStatusWithAuth(childId, userId, status);
    }

    private void createSystemNoteForAssignment(AssignedMission mission, User therapist) {
        String title = String.format("미션 할당: %s", mission.getTemplate().getTitle());
        String content = String.format(
                """
                **미션이 할당되었습니다**

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
                mission.getDueDate() != null ? mission.getDueDate().toString() : "미정",
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
                **미션이 완료되었습니다**

                - 미션: %s
                - 완료일: %s
                - 완료자: %s
                - 부모 코멘트: %s
                - 첨부 사진 수: %d개
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
                **미션 검증이 완료되었습니다**

                - 미션: %s
                - 검증일: %s
                - 검증 치료사: %s
                - 치료사 피드백: %s
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
