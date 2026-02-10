package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.mission.AssignedMission;
import com.planB.myexpressionfriend.common.domain.mission.MissionStatus;
import com.planB.myexpressionfriend.common.domain.mission.MissionTemplate;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.dto.mission.*;
import com.planB.myexpressionfriend.common.dto.note.PageResponseDTO;
import com.planB.myexpressionfriend.common.repository.AssignedMissionRepository;
import com.planB.myexpressionfriend.common.repository.ChildRepository;
import com.planB.myexpressionfriend.common.repository.MissionTemplateRepository;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AssignedMission Service
 *
 * 책임:
 * - 미션 할당/조회/상태 변경
 * - 권한 검증 (VIEW_REPORT, WRITE_NOTE)
 * - 시스템 노트 자동 생성
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AssignedMissionService {

    private final AssignedMissionRepository missionRepository;
    private final MissionTemplateRepository templateRepository;
    private final ChildRepository childRepository;
    private final UserRepository userRepository;
    private final ChildNoteService noteService;

    // ============= 미션 할당 =============

    /**
     * 미션 할당 (치료사만 가능)
     *
     * @param dto 할당 요청 DTO
     * @param therapistId 치료사 ID
     * @return 할당된 미션 DTO
     */
    @Transactional
    public AssignedMissionDTO assignMission(AssignedMissionCreateDTO dto, UUID therapistId) {
        log.info("미션 할당 시작 - childId: {}, templateId: {}, therapistId: {}",
                dto.getChildId(), dto.getTemplateId(), therapistId);

        // 1. 아동 조회
        Child child = childRepository.findById(dto.getChildId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아동입니다"));

        // 2. 치료사 조회
        User therapist = userRepository.findById(therapistId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));

        // 3. 템플릿 조회
        MissionTemplate template = templateRepository.findByIdAndActive(dto.getTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 비활성화된 템플릿입니다"));

        // 4. 권한 검증: 치료사가 해당 아동에 접근 가능한지
        if (!child.canAccess(therapistId)) {
            log.warn("미션 할당 권한 없음 - childId: {}, therapistId: {}",
                    dto.getChildId(), therapistId);
            throw new AccessDeniedException("해당 아동에 대한 권한이 없습니다");
        }

        // 5. 미션 생성
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

        // 6. 시스템 노트 자동 생성 (미션 할당 기록)
        createSystemNoteForAssignment(savedMission, therapist);

        log.info("미션 할당 완료 - missionId: {}", savedMission.getMissionId());
        return AssignedMissionDTO.from(savedMission);
    }

    // ============= 미션 조회 =============

    /**
     * 미션 상세 조회
     * 권한: VIEW_REPORT 필요
     *
     * @param missionId 미션 ID
     * @param userId 조회 요청 사용자 ID
     * @return 미션 상세 DTO
     */
    public AssignedMissionDetailDTO getMission(UUID missionId, UUID userId) {
        log.debug("미션 조회 - missionId: {}, userId: {}", missionId, userId);

        AssignedMission mission = missionRepository.findByIdWithAuth(missionId, userId)
                .orElseThrow(() -> new AccessDeniedException("미션 조회 권한이 없거나 존재하지 않는 미션입니다"));

        return AssignedMissionDetailDTO.from(mission);
    }

    /**
     * 아동의 미션 목록 조회 (페이징)
     * 권한: VIEW_REPORT 필요
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @param pageable 페이징 정보
     * @return 미션 목록 (페이징)
     */
    public PageResponseDTO<AssignedMissionDTO> getMissionsByChild(
            UUID childId,
            UUID userId,
            Pageable pageable
    ) {
        log.debug("아동 미션 목록 조회 - childId: {}, userId: {}, page: {}",
                childId, userId, pageable.getPageNumber());

        Page<AssignedMission> missionPage =
                missionRepository.findByChildIdWithAuth(childId, userId, pageable);

        return PageResponseDTO.from(missionPage, AssignedMissionDTO::from);
    }

    /**
     * 미션 검색/필터링
     *
     * @param searchDTO 검색 조건
     * @param userId 조회 요청 사용자 ID
     * @return 검색 결과 (페이징)
     */
    public PageResponseDTO<AssignedMissionDTO> searchMissions(
            MissionSearchDTO searchDTO,
            UUID userId
    ) {
        log.debug("미션 검색 - childId: {}, status: {}, userId: {}",
                searchDTO.getChildId(), searchDTO.getStatus(), userId);

        searchDTO.validate();
        Pageable pageable = searchDTO.toPageable();

        Page<AssignedMission> missionPage;

        // 검색 조건에 따라 다른 Repository 메서드 호출
        if (searchDTO.getStatus() != null) {
            // 상태별 필터링
            missionPage = missionRepository.findByChildIdAndStatusWithAuth(
                    searchDTO.getChildId(),
                    userId,
                    searchDTO.getStatus(),
                    pageable
            );
        } else if (searchDTO.getTherapistId() != null) {
            // 치료사별 필터링
            missionPage = missionRepository.findByChildIdAndTherapistWithAuth(
                    searchDTO.getChildId(),
                    userId,
                    searchDTO.getTherapistId(),
                    pageable
            );
        } else if (searchDTO.getStartDate() != null && searchDTO.getEndDate() != null) {
            // 날짜 범위 필터링
            missionPage = missionRepository.findByChildIdAndDateRangeWithAuth(
                    searchDTO.getChildId(),
                    userId,
                    searchDTO.getStartDate(),
                    searchDTO.getEndDate(),
                    pageable
            );
        } else {
            // 조건 없음 - 전체 조회
            missionPage = missionRepository.findByChildIdWithAuth(
                    searchDTO.getChildId(),
                    userId,
                    pageable
            );
        }

        return PageResponseDTO.from(missionPage, AssignedMissionDTO::from);
    }

    /**
     * 마감일 지난 미션 조회
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @param pageable 페이징 정보
     * @return 마감일 지난 미션 목록
     */
    public PageResponseDTO<AssignedMissionDTO> getOverdueMissions(
            UUID childId,
            UUID userId,
            Pageable pageable
    ) {
        log.debug("마감일 지난 미션 조회 - childId: {}, userId: {}", childId, userId);

        Page<AssignedMission> missionPage = missionRepository.findOverdueMissionsWithAuth(
                childId,
                userId,
                LocalDateTime.now(),
                pageable
        );

        return PageResponseDTO.from(missionPage, AssignedMissionDTO::from);
    }

    /**
     * 완료 대기중인 미션 조회 (치료사 검증용)
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID (치료사)
     * @param pageable 페이징 정보
     * @return 완료 대기 미션 목록
     */
    public PageResponseDTO<AssignedMissionDetailDTO> getPendingVerificationMissions(
            UUID childId,
            UUID userId,
            Pageable pageable
    ) {
        log.debug("완료 대기 미션 조회 - childId: {}, userId: {}", childId, userId);

        Page<AssignedMission> missionPage =
                missionRepository.findPendingVerificationWithAuth(childId, userId, pageable);

        return PageResponseDTO.from(missionPage, AssignedMissionDetailDTO::from);
    }

    // ============= 미션 상태 변경 =============

    /**
     * 미션 시작 (부모만 가능)
     * ASSIGNED → IN_PROGRESS
     *
     * @param missionId 미션 ID
     * @param userId 사용자 ID
     * @return 수정된 미션 DTO
     */
    @Transactional
    public AssignedMissionDTO startMission(UUID missionId, UUID userId) {
        log.info("미션 시작 - missionId: {}, userId: {}", missionId, userId);

        // 1. 미션 조회 (권한 검증 포함)
        AssignedMission mission = missionRepository.findByIdWithAuth(missionId, userId)
                .orElseThrow(() -> new AccessDeniedException("미션 조회 권한이 없거나 존재하지 않는 미션입니다"));

        // 2. 시작 권한 확인 (부모 + WRITE_NOTE 권한)
        if (!mission.canStart(userId)) {
            log.warn("미션 시작 권한 없음 - missionId: {}, userId: {}", missionId, userId);
            throw new AccessDeniedException("미션을 시작할 권한이 없습니다");
        }

        // 3. 상태 변경
        mission.start();

        log.info("미션 시작 완료 - missionId: {}", missionId);
        return AssignedMissionDTO.from(mission);
    }

    /**
     * 미션 완료 (부모만 가능)
     * IN_PROGRESS → COMPLETED
     *
     * @param missionId 미션 ID
     * @param dto 상태 변경 요청 DTO
     * @param userId 사용자 ID
     * @return 수정된 미션 DTO
     */
    @Transactional
    public AssignedMissionDTO completeMission(
            UUID missionId,
            MissionStatusUpdateDTO dto,
            UUID userId
    ) {
        log.info("미션 완료 - missionId: {}, userId: {}", missionId, userId);

        // 1. 미션 조회 (권한 검증 포함)
        AssignedMission mission = missionRepository.findByIdWithAuth(missionId, userId)
                .orElseThrow(() -> new AccessDeniedException("미션 조회 권한이 없거나 존재하지 않는 미션입니다"));

        // 2. 완료 권한 확인 (부모 + WRITE_NOTE 권한)
        if (!mission.canComplete(userId)) {
            log.warn("미션 완료 권한 없음 - missionId: {}, userId: {}", missionId, userId);
            throw new AccessDeniedException("미션을 완료할 권한이 없습니다");
        }

        // 3. 상태 변경
        mission.complete(dto.getParentNote());

        // 4. 시스템 노트 자동 생성 (미션 완료 기록)
        User parent = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));
        createSystemNoteForCompletion(mission, parent);

        log.info("미션 완료 처리 완료 - missionId: {}", missionId);
        return AssignedMissionDTO.from(mission);
    }

    /**
     * 미션 검증 (치료사만 가능)
     * COMPLETED → VERIFIED
     *
     * @param missionId 미션 ID
     * @param dto 상태 변경 요청 DTO
     * @param userId 사용자 ID (치료사)
     * @return 수정된 미션 DTO
     */
    @Transactional
    public AssignedMissionDTO verifyMission(
            UUID missionId,
            MissionStatusUpdateDTO dto,
            UUID userId
    ) {
        log.info("미션 검증 - missionId: {}, userId: {}", missionId, userId);

        // 1. 미션 조회 (권한 검증 포함)
        AssignedMission mission = missionRepository.findByIdWithAuth(missionId, userId)
                .orElseThrow(() -> new AccessDeniedException("미션 조회 권한이 없거나 존재하지 않는 미션입니다"));

        // 2. 검증 권한 확인 (할당한 치료사만)
        if (!mission.canVerify(userId)) {
            log.warn("미션 검증 권한 없음 - missionId: {}, userId: {}", missionId, userId);
            throw new AccessDeniedException("미션을 검증할 권한이 없습니다");
        }

        // 3. 상태 변경
        mission.verify(dto.getTherapistFeedback());

        // 4. 시스템 노트 자동 생성 (미션 검증 완료 기록)
        User therapist = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));
        createSystemNoteForVerification(mission, therapist);

        log.info("미션 검증 완료 - missionId: {}", missionId);
        return AssignedMissionDTO.from(mission);
    }

    /**
     * 미션 취소 (할당한 치료사만 가능)
     *
     * @param missionId 미션 ID
     * @param userId 사용자 ID (치료사)
     */
    @Transactional
    public void cancelMission(UUID missionId, UUID userId) {
        log.info("미션 취소 - missionId: {}, userId: {}", missionId, userId);

        // 1. 미션 조회 (권한 검증 포함)
        AssignedMission mission = missionRepository.findByIdWithAuth(missionId, userId)
                .orElseThrow(() -> new AccessDeniedException("미션 조회 권한이 없거나 존재하지 않는 미션입니다"));

        // 2. 취소 권한 확인 (할당한 치료사만)
        if (!mission.canCancel(userId)) {
            log.warn("미션 취소 권한 없음 - missionId: {}, userId: {}", missionId, userId);
            throw new AccessDeniedException("미션을 취소할 권한이 없습니다");
        }

        // 3. 상태 변경
        mission.cancel();

        log.info("미션 취소 완료 - missionId: {}", missionId);
    }

    // ============= 미션 삭제 =============

    /**
     * 미션 삭제 (Soft Delete)
     * 권한: 할당한 치료사만
     *
     * @param missionId 미션 ID
     * @param userId 사용자 ID (치료사)
     */
    @Transactional
    public void deleteMission(UUID missionId, UUID userId) {
        log.info("미션 삭제 시작 - missionId: {}, userId: {}", missionId, userId);

        // 1. 미션 조회 (권한 검증 포함)
        AssignedMission mission = missionRepository.findByIdWithAuth(missionId, userId)
                .orElseThrow(() -> new AccessDeniedException("미션 조회 권한이 없거나 존재하지 않는 미션입니다"));

        // 2. 삭제 권한 확인 (할당한 치료사만)
        if (!mission.isTherapist(userId)) {
            log.warn("미션 삭제 권한 없음 - missionId: {}, userId: {}", missionId, userId);
            throw new AccessDeniedException("미션을 삭제할 권한이 없습니다");
        }

        // 3. Soft Delete
        mission.delete();
        log.info("미션 삭제 완료 - missionId: {}", missionId);
    }

    // ============= 통계 =============

    /**
     * 아동의 미션 총 개수
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @return 미션 개수
     */
    public long countMissionsByChild(UUID childId, UUID userId) {
        return missionRepository.countByChildIdWithAuth(childId, userId);
    }

    /**
     * 상태별 미션 개수
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @param status 미션 상태
     * @return 미션 개수
     */
    public long countMissionsByChildAndStatus(
            UUID childId,
            UUID userId,
            MissionStatus status
    ) {
        return missionRepository.countByChildIdAndStatusWithAuth(childId, userId, status);
    }

    // ============= 시스템 노트 자동 생성 =============

    /**
     * 미션 할당 시 시스템 노트 생성
     */
    private void createSystemNoteForAssignment(AssignedMission mission, User therapist) {
        String title = String.format("미션 할당: %s", mission.getTemplate().getTitle());
        String content = String.format(
                "**미션이 할당되었습니다**\n\n" +
                        "- 미션: %s\n" +
                        "- 카테고리: %s\n" +
                        "- 난이도: %s\n" +
                        "- 할당 치료사: %s\n" +
                        "- 목표 완료일: %s\n\n" +
                        "%s",
                mission.getTemplate().getTitle(),
                mission.getTemplate().getCategory().getDisplayName(),
                mission.getTemplate().getDifficulty().getDisplayName(),
                therapist.getName(),
                mission.getDueDate() != null ? mission.getDueDate().toString() : "미정",
                mission.getTemplate().getInstructions()
        );

        noteService.createSystemNote(
                mission.getChild().getChildId(),
                therapist.getUserId(),
                title,
                content
        );
    }

    /**
     * 미션 완료 시 시스템 노트 생성
     */
    private void createSystemNoteForCompletion(AssignedMission mission, User parent) {
        String title = String.format("미션 완료: %s", mission.getTemplate().getTitle());
        String content = String.format(
                "**미션이 완료되었습니다**\n\n" +
                        "- 미션: %s\n" +
                        "- 완료일: %s\n" +
                        "- 완료자: %s\n" +
                        "- 부모 코멘트: %s\n" +
                        "- 첨부 사진: %d개",
                mission.getTemplate().getTitle(),
                mission.getCompletedAt(),
                parent.getName(),
                mission.getParentNote() != null ? mission.getParentNote() : "없음",
                mission.getPhotos().size()
        );

        noteService.createSystemNote(
                mission.getChild().getChildId(),
                parent.getUserId(),
                title,
                content
        );
    }

    /**
     * 미션 검증 완료 시 시스템 노트 생성
     */
    private void createSystemNoteForVerification(AssignedMission mission, User therapist) {
        String title = String.format("미션 검증 완료: %s", mission.getTemplate().getTitle());
        String content = String.format(
                "**미션 검증이 완료되었습니다**\n\n" +
                        "- 미션: %s\n" +
                        "- 검증일: %s\n" +
                        "- 검증 치료사: %s\n" +
                        "- 치료사 피드백: %s",
                mission.getTemplate().getTitle(),
                mission.getVerifiedAt(),
                therapist.getName(),
                mission.getTherapistFeedback() != null ? mission.getTherapistFeedback() : "없음"
        );

        noteService.createSystemNote(
                mission.getChild().getChildId(),
                therapist.getUserId(),
                title,
                content
        );
    }
}