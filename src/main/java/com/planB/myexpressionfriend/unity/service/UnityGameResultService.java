package com.planB.myexpressionfriend.unity.service;

import com.planB.myexpressionfriend.common.domain.game.GameSession;
import com.planB.myexpressionfriend.common.exception.InvalidRequestException;
import com.planB.myexpressionfriend.common.repository.GameSessionRepository;
import com.planB.myexpressionfriend.unity.domain.UnityGameResult;
import com.planB.myexpressionfriend.unity.dto.UnityGameResultSaveRequestDTO;
import com.planB.myexpressionfriend.unity.dto.UnityGameResultSaveResponseDTO;
import com.planB.myexpressionfriend.unity.repository.UnityGameResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Unity 게임 결과 저장 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnityGameResultService {

    private final UnityGameResultRepository unityGameResultRepository;
    private final GameSessionRepository gameSessionRepository;

    /**
     * Unity 게임 결과를 저장합니다.
     * sessionToken으로 GameSession을 조회하여 Child와 연결합니다.
     *
     * @param requestDTO 저장 요청 데이터
     * @return UnityGameResultSaveResponseDTO
     */
    @Transactional
    public UnityGameResultSaveResponseDTO saveResult(UnityGameResultSaveRequestDTO requestDTO) {
        GameSession session = gameSessionRepository
                .findValidSessionByToken(requestDTO.getSessionToken(), LocalDateTime.now())
                .orElseThrow(() -> new InvalidRequestException("유효하지 않거나 만료된 게임 세션입니다."));

        UnityGameResult toSave = UnityGameResult.builder()
                .gameSession(session)
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
                .childId(session.getChild().getChildId())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
