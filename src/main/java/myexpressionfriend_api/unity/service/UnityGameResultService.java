package myexpressionfriend_api.unity.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.exception.InvalidRequestException;
import myexpressionfriend_api.game.domain.GameSession;
import myexpressionfriend_api.game.repository.GameSessionRepository;
import myexpressionfriend_api.unity.domain.UnityGameResult;
import myexpressionfriend_api.unity.dto.UnityGameResultSaveRequestDTO;
import myexpressionfriend_api.unity.dto.UnityGameResultSaveResponseDTO;
import myexpressionfriend_api.unity.repository.UnityGameResultRepository;
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
     * Unity 게임 결과 저장
     * - sessionToken으로 GameSession을 조회하여 Child와 연결
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
