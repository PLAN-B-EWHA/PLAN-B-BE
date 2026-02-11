package com.planB.myexpressionfriend.common.service;


import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.game.GameSession;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.dto.game.GameSessionDTO;
import com.planB.myexpressionfriend.common.repository.ChildRepository;
import com.planB.myexpressionfriend.common.repository.GameSessionRepository;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GameSession Service
 *
 * 책임:
 * - 게임 세션 생성/조회/종료
 * - 세션 유효성 검증
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GameSessionService {

    private final GameSessionRepository sessionRepository;
    private final ChildRepository childRepository;
    private final UserRepository userRepository;

    // ============= 세션 생성 =============

    /**
     * PIN 검증 후 게임 세션 생성
     *
     * @param childId 아동 ID
     * @param userId 인증한 사용자 ID
     * @return 게임 세션 DTO
     */
    @Transactional
    public GameSessionDTO createSession(UUID childId, UUID userId) {
        log.info("게임 세션 생성 - childId: {}, userId: {}", childId, userId);

        // 1. 아동 조회
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아동입니다"));

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));

        // 3. PLAY_GAME 권한 확인 ✅
        if (!child.hasPermission(userId, ChildPermissionType.PLAY_GAME)) {
            log.warn("게임 세션 생성 권한 없음 - childId: {}, userId: {}", childId, userId);
            throw new AccessDeniedException("게임 플레이 권한이 없습니다");
        }

        // 4. 기존 활성 세션 종료 (중복 방지)
        sessionRepository.terminateAllSessionsByChildId(childId);

        // 5. 새 세션 생성
        GameSession session = GameSession.create(child, user);
        GameSession savedSession = sessionRepository.save(session);

        log.info("게임 세션 생성 완료 - sessionId: {}, token: {}",
                savedSession.getSessionId(), savedSession.getSessionToken());

        return GameSessionDTO.from(savedSession);
    }

    // ============= 세션 검증 =============

    /**
     * 세션 토큰 검증
     *
     * @param sessionToken 세션 토큰
     * @return 유효한 세션 DTO
     */
    public GameSessionDTO validateSession(String sessionToken) {
        log.debug("세션 토큰 검증 - token: {}", sessionToken);

        GameSession session = sessionRepository.findValidSessionByToken(
                        sessionToken, LocalDateTime.now())
                .orElseThrow(() -> new AccessDeniedException("유효하지 않은 세션입니다"));

        return GameSessionDTO.from(session);
    }

    /**
     * 세션 갱신 (마지막 사용 시간 업데이트)
     *
     * @param sessionToken 세션 토큰
     */
    @Transactional
    public void refreshSession(String sessionToken) {
        log.debug("세션 갱신 - token: {}", sessionToken);

        GameSession session = sessionRepository.findValidSessionByToken(
                        sessionToken, LocalDateTime.now())
                .orElseThrow(() -> new AccessDeniedException("유효하지 않은 세션입니다"));

        session.refresh();
    }

    // ============= 세션 조회 =============

    /**
     * 아동의 활성 세션 목록
     *
     * @param childId 아동 ID
     * @param userId 요청 사용자 ID
     * @return 활성 세션 목록
     */
    public List<GameSessionDTO> getActiveSessionsByChild(UUID childId, UUID userId) {
        log.debug("아동 활성 세션 조회 - childId: {}", childId);

        // 권한 확인
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아동입니다"));

        if (!child.canAccess(userId)) {
            throw new AccessDeniedException("해당 아동에 대한 권한이 없습니다");
        }

        List<GameSession> sessions = sessionRepository.findActiveSessionsByChildId(
                childId, LocalDateTime.now());

        return sessions.stream()
                .map(GameSessionDTO::from)
                .collect(Collectors.toList());
    }

    // ============= 세션 종료 =============

    /**
     * 세션 종료
     *
     * @param sessionToken 세션 토큰
     */
    @Transactional
    public void terminateSession(String sessionToken) {
        log.info("세션 종료 - token: {}", sessionToken);

        GameSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 세션입니다"));

        session.terminate();
        log.info("세션 종료 완료 - sessionId: {}", session.getSessionId());
    }

    /**
     * 아동의 모든 세션 종료
     *
     * @param childId 아동 ID
     * @param userId 요청 사용자 ID
     */
    @Transactional
    public void terminateAllSessionsByChild(UUID childId, UUID userId) {
        log.info("아동의 모든 세션 종료 - childId: {}", childId);

        // 권한 확인
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아동입니다"));

        if (!child.isPrimaryParent(userId)) {
            throw new AccessDeniedException("주보호자만 세션을 종료할 수 있습니다");
        }

        sessionRepository.terminateAllSessionsByChildId(childId);
        log.info("아동의 모든 세션 종료 완료 - childId: {}", childId);
    }

    // ============= 배치 작업 =============

    /**
     * 만료된 세션 삭제 (스케줄러에서 호출)
     */
    @Transactional
    public void cleanupExpiredSessions() {
        log.info("만료된 세션 정리 시작");

        // 7일 이전 만료 세션 삭제
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
        sessionRepository.deleteExpiredSessions(cutoffTime);

        log.info("만료된 세션 정리 완료");
    }
}
