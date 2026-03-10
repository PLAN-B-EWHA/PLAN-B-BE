package com.planB.myexpressionfriend.unity.service;

import com.planB.myexpressionfriend.unity.domain.UnityGameResult;
import com.planB.myexpressionfriend.unity.dto.UnityGameResultSaveRequestDTO;
import com.planB.myexpressionfriend.unity.dto.UnityGameResultSaveResponseDTO;
import com.planB.myexpressionfriend.unity.repository.UnityGameResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unity 게임 결과 저장 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnityGameResultService {

    private final UnityGameResultRepository unityGameResultRepository;

    /**
     * Unity 게임 결과를 저장합니다.
     *
     * @param requestDTO 저장 요청 데이터
     * @return UnityGameResultSaveResponseDTO
     */
    @Transactional
    public UnityGameResultSaveResponseDTO saveResult(UnityGameResultSaveRequestDTO requestDTO) {
        UnityGameResult toSave = UnityGameResult.builder()
                .missionId(requestDTO.getMissionId())
                .success(requestDTO.getSuccess())
                .score(requestDTO.getScore())
                .durationSeconds(requestDTO.getDurationSeconds())
                .retryCount(requestDTO.getRetryCount())
                .build();

        UnityGameResult saved = unityGameResultRepository.save(toSave);

        return UnityGameResultSaveResponseDTO.builder()
                .savedId(saved.getUnityGameResultId())
                .missionId(saved.getMissionId())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}