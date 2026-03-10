package com.planB.myexpressionfriend.unity.service;

import com.planB.myexpressionfriend.unity.domain.UnityMission;
import com.planB.myexpressionfriend.unity.dto.UnityMissionImportRequestDTO;
import com.planB.myexpressionfriend.unity.dto.UnityMissionImportResultDTO;
import com.planB.myexpressionfriend.unity.dto.UnityMissionItemDTO;
import com.planB.myexpressionfriend.unity.dto.UnityMissionResponseDTO;
import com.planB.myexpressionfriend.unity.repository.UnityMissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unity 미션 저장 및 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnityMissionService {

    private final UnityMissionRepository unityMissionRepository;

    /**
     * Unity 미션 목록을 저장합니다.
     *
     * @param requestDTO 가져오기 요청 데이터
     * @return UnityMissionImportResultDTO
     */
    @Transactional
    public UnityMissionImportResultDTO importMissions(UnityMissionImportRequestDTO requestDTO) {
        List<UnityMission> toSave = requestDTO.getMissions().stream()
                .map(this::toEntity)
                .toList();

        List<UnityMission> saved = unityMissionRepository.saveAll(toSave);
        List<Long> savedIds = saved.stream()
                .map(UnityMission::getUnityMissionId)
                .toList();

        return UnityMissionImportResultDTO.builder()
                .requestedCount(requestDTO.getMissions().size())
                .savedCount(saved.size())
                .savedIds(savedIds)
                .build();
    }

    /**
     * 최근 저장된 Unity 미션을 조회합니다.
     *
     * @param limit 조회 개수
     * @return List<UnityMissionResponseDTO>
     */
    public List<UnityMissionResponseDTO> getLatestMissions(int limit) {
        int size = Math.min(Math.max(limit, 1), 100);

        return unityMissionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, size))
                .map(UnityMissionResponseDTO::from)
                .getContent();
    }

    /**
     * Unity 런타임에서 사용할 최신 미션 목록을 조회합니다.
     *
     * @return List<UnityMissionResponseDTO>
     */
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
     * 미션 DTO를 엔티티로 변환합니다.
     *
     * @param dto Unity 미션 항목
     * @return UnityMission
     */
    private UnityMission toEntity(UnityMissionItemDTO dto) {
        return UnityMission.builder()
                .missionId(dto.getMissionId())
                .missionName(dto.getMissionName())
                .missionTypeString(dto.getMissionTypeString())
                .targetKeyword(dto.getTargetKeyword())
                .targetEmotionString(dto.getTargetEmotionString())
                .expressionData(dto.getExpressionData())
                .situationData(dto.getSituationData())
                .build();
    }
}