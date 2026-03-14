package com.planB.myexpressionfriend.unity.service;

import com.planB.myexpressionfriend.unity.domain.UnityExpressionMissionDetail;
import com.planB.myexpressionfriend.unity.domain.UnityMission;
import com.planB.myexpressionfriend.unity.domain.UnitySituationMissionDetail;
import com.planB.myexpressionfriend.unity.dto.UnityMissionImportRequestDTO;
import com.planB.myexpressionfriend.unity.dto.UnityMissionImportResultDTO;
import com.planB.myexpressionfriend.unity.dto.UnityMissionItemDTO;
import com.planB.myexpressionfriend.unity.dto.UnityMissionResponseDTO;
import com.planB.myexpressionfriend.unity.repository.UnityExpressionMissionDetailRepository;
import com.planB.myexpressionfriend.unity.repository.UnityMissionRepository;
import com.planB.myexpressionfriend.unity.repository.UnitySituationMissionDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnityMissionService {

    private final UnityMissionRepository unityMissionRepository;
    private final UnityExpressionMissionDetailRepository unityExpressionMissionDetailRepository;
    private final UnitySituationMissionDetailRepository unitySituationMissionDetailRepository;

    @Transactional
    public UnityMissionImportResultDTO importMissions(UnityMissionImportRequestDTO requestDTO) {
        List<Long> savedIds = new ArrayList<>();

        for (UnityMissionItemDTO dto : requestDTO.getMissions()) {
            UnityMission mission = unityMissionRepository.save(toEntity(dto));
            saveMissionDetail(mission, dto);
            savedIds.add(mission.getUnityMissionId());
        }

        return UnityMissionImportResultDTO.builder()
                .requestedCount(requestDTO.getMissions().size())
                .savedCount(savedIds.size())
                .savedIds(savedIds)
                .build();
    }

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

    private UnityMission toEntity(UnityMissionItemDTO dto) {
        return UnityMission.builder()
                .missionId(dto.getMissionId())
                .missionName(dto.getMissionName())
                .missionTypeString(dto.getMissionTypeString())
                .targetKeyword(dto.getTargetKeyword())
                .targetEmotionString(dto.getTargetEmotionString())
                .build();
    }

    private void saveMissionDetail(UnityMission mission, UnityMissionItemDTO dto) {
        if (dto.getExpressionData() != null && !dto.getExpressionData().isNull()) {
            unityExpressionMissionDetailRepository.save(UnityExpressionMissionDetail.builder()
                    .mission(mission)
                    .expressionData(dto.getExpressionData())
                    .build());
        }

        if (dto.getSituationData() != null && !dto.getSituationData().isNull()) {
            unitySituationMissionDetailRepository.save(UnitySituationMissionDetail.builder()
                    .mission(mission)
                    .situationData(dto.getSituationData())
                    .build());
        }
    }
}