package com.planB.myexpressionfriend.unity.service;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.game.GameSession;
import com.planB.myexpressionfriend.common.exception.EntityNotFoundException;
import com.planB.myexpressionfriend.common.repository.ChildRepository;
import com.planB.myexpressionfriend.common.repository.GameSessionRepository;
import com.planB.myexpressionfriend.unity.domain.UnityMission;
import com.planB.myexpressionfriend.unity.domain.UnityMissionApprovalStatus;
import com.planB.myexpressionfriend.unity.dto.UnityMissionImportRequestDTO;
import com.planB.myexpressionfriend.unity.dto.UnityMissionImportResultDTO;
import com.planB.myexpressionfriend.unity.dto.UnityMissionItemDTO;
import com.planB.myexpressionfriend.unity.dto.UnityMissionResponseDTO;
import com.planB.myexpressionfriend.unity.repository.UnityMissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UnityMissionService {

    private final UnityMissionRepository unityMissionRepository;
    private final ChildRepository childRepository;
    private final GameSessionRepository gameSessionRepository;

    // self-invocation 문제 해결: @Cacheable이 적용된 메서드를 같은 빈 내부에서 호출할 때
    // Spring AOP 프록시를 거치도록 자기 자신을 @Lazy로 주입
    @Autowired
    @Lazy
    private UnityMissionService self;

    // ── 임포트 (child 없는 레거시 경로) ──────────────────────────────────

    @Transactional
    public UnityMissionImportResultDTO importMissions(UnityMissionImportRequestDTO requestDTO) {
        List<Long> savedIds = new ArrayList<>();
        for (UnityMissionItemDTO dto : requestDTO.getMissions()) {
            UnityMission mission = unityMissionRepository.save(toEntity(dto, null, null));
            savedIds.add(mission.getUnityMissionId());
        }
        return buildImportResult(requestDTO.getMissions().size(), savedIds);
    }

    // ── 아동 맞춤 저장 (LLM 생성 경로) ──────────────────────────────────

    @Transactional
    public UnityMissionImportResultDTO importMissionsForChild(
            UnityMissionImportRequestDTO requestDTO, UUID childId, LocalDate missionDate) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동을 찾을 수 없습니다."));

        List<Long> savedIds = new ArrayList<>();
        for (UnityMissionItemDTO dto : requestDTO.getMissions()) {
            UnityMission mission = unityMissionRepository.save(toEntity(dto, child, missionDate));
            savedIds.add(mission.getUnityMissionId());
        }
        return buildImportResult(requestDTO.getMissions().size(), savedIds);
    }

    // ── 조회 ─────────────────────────────────────────────────────────────

    public List<UnityMissionResponseDTO> getLatestMissions(int limit) {
        int size = Math.min(Math.max(limit, 1), 100);
        return unityMissionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, size))
                .map(UnityMissionResponseDTO::from)
                .getContent();
    }

    public List<UnityMissionResponseDTO> getMissionsForUnity() {
        List<UnityMission> all = unityMissionRepository.findAllByOrderByMissionIdAscCreatedAtDesc();
        Map<Integer, UnityMission> latestByMissionId = new LinkedHashMap<>();
        for (UnityMission mission : all) {
            latestByMissionId.putIfAbsent(mission.getMissionId(), mission);
        }
        return latestByMissionId.values().stream()
                .map(UnityMissionResponseDTO::from)
                .toList();
    }

    /**
     * 게임 클라이언트용: 세션 토큰으로 해당 아동의 오늘 승인된 미션 반환 (캐시 적용)
     */
    public List<UnityMissionResponseDTO> getApprovedMissionsForUnity(String sessionToken) {
        GameSession session = gameSessionRepository
                .findValidSessionByToken(sessionToken, LocalDateTime.now())
                .orElseThrow(() -> new AccessDeniedException("유효하지 않은 세션입니다."));

        UUID childId = session.getChild().getChildId();
        // self 호출로 @Cacheable 프록시가 적용되도록 함
        return self.getApprovedMissionsFromCache(childId, LocalDate.now());
    }

    @Cacheable(value = "approvedMissions", key = "#childId + ':' + #date")
    public List<UnityMissionResponseDTO> getApprovedMissionsFromCache(UUID childId, LocalDate date) {
        log.debug("[CACHE MISS] DB 조회 - childId={}, date={}", childId, date);
        return unityMissionRepository
                .findByChild_ChildIdAndApprovalStatusAndMissionDate(
                        childId, UnityMissionApprovalStatus.APPROVED, date)
                .stream()
                .map(UnityMissionResponseDTO::from)
                .toList();
    }

    // ── 승인 / 거절 ───────────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "approvedMissions", allEntries = true)
    public UnityMissionResponseDTO approveMission(Long missionId, UUID userId) {
        UnityMission mission = findMissionWithPermissionCheck(missionId, userId);
        mission.approve(userId);
        return UnityMissionResponseDTO.from(mission);
    }

    @Transactional
    @CacheEvict(value = "approvedMissions", allEntries = true)
    public UnityMissionResponseDTO rejectMission(Long missionId, UUID userId, String reason) {
        UnityMission mission = findMissionWithPermissionCheck(missionId, userId);
        mission.reject(userId, reason);
        return UnityMissionResponseDTO.from(mission);
    }

    // ── private ───────────────────────────────────────────────────────────

    private UnityMission findMissionWithPermissionCheck(Long missionId, UUID userId) {
        UnityMission mission = unityMissionRepository.findById(missionId)
                .orElseThrow(() -> new EntityNotFoundException("미션을 찾을 수 없습니다."));

        Child child = mission.getChild();
        if (child == null) {
            throw new IllegalStateException("아동이 연결되지 않은 미션입니다.");
        }
        if (!child.hasPermission(userId, ChildPermissionType.PLAY_GAME)) {
            throw new AccessDeniedException("미션 승인/거절 권한이 없습니다.");
        }
        return mission;
    }

    private UnityMission toEntity(UnityMissionItemDTO dto, Child child, LocalDate missionDate) {
        return UnityMission.builder()
                .child(child)
                .missionId(dto.getMissionId())
                .missionName(dto.getMissionName())
                .missionTypeString(dto.getMissionTypeString())
                .targetKeyword(dto.getTargetKeyword())
                .targetEmotionString(dto.getTargetEmotionString())
                .expressionData(dto.getExpressionData())
                .situationData(dto.getSituationData())
                .missionDate(missionDate)
                .build();
    }

    private UnityMissionImportResultDTO buildImportResult(int requested, List<Long> savedIds) {
        return UnityMissionImportResultDTO.builder()
                .requestedCount(requested)
                .savedCount(savedIds.size())
                .savedIds(savedIds)
                .build();
    }
}
