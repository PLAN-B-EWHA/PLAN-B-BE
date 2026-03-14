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
import com.planB.myexpressionfriend.common.exception.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 게임 세션 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GameSessionService {

    private final GameSessionRepository sessionRepository;
    private final ChildRepository childRepository;
    private final UserRepository userRepository;

    /**
     */
    @Transactional
    public GameSessionDTO createSession(UUID childId, UUID userId) {
        // 비관적 락으로 Child 행을 선점해 동시 세션 생성 race condition을 방지합니다.
        // 같은 childId로 들어온 동시 요청은 이 지점에서 순차적으로 처리됩니다.
        Child child = childRepository.findByIdForUpdate(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        if (!child.hasPermission(userId, ChildPermissionType.PLAY_GAME)) {
            throw new AccessDeniedException("게임 플레이 권한이 없습니다.");
        }

        // 기존 활성 세션 정리 후 신규 세션 생성 (락 보유 중이므로 원자적으로 처리됨)
        sessionRepository.terminateAllSessionsByChildId(childId);

        GameSession session = GameSession.create(child, user);
        GameSession savedSession = sessionRepository.save(session);

        return GameSessionDTO.from(savedSession, true);
    }

    /**
     * 세션 토큰 유효성 검증
     */
    public GameSessionDTO validateSession(String sessionToken) {
        GameSession session = sessionRepository.findValidSessionByToken(sessionToken, LocalDateTime.now())
                .orElseThrow(() -> new AccessDeniedException("유효하지 않은 세션입니다."));

        return GameSessionDTO.from(session, false);
    }

    /**
     */
    @Transactional
    public void refreshSession(String sessionToken) {
        GameSession session = sessionRepository.findValidSessionByToken(sessionToken, LocalDateTime.now())
                .orElseThrow(() -> new AccessDeniedException("유효하지 않은 세션입니다."));
        session.refresh();
    }

    /**
     */
    public List<GameSessionDTO> getActiveSessionsByChild(UUID childId, UUID userId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동을 찾을 수 없습니다."));

        if (!child.canAccess(userId)) {
            throw new AccessDeniedException("해당 아동에 대한 접근 권한이 없습니다.");
        }

        return sessionRepository.findActiveSessionsByChildId(childId, LocalDateTime.now()).stream()
                .map(session -> GameSessionDTO.from(session, false))
                .collect(Collectors.toList());
    }

    /**
     */
    @Transactional
    public void terminateSession(String sessionToken, UUID requesterUserId) {
        GameSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new EntityNotFoundException("세션을 찾을 수 없습니다."));

        Child child = session.getChild();
        boolean isCreator = session.getAuthenticatedBy().getUserId().equals(requesterUserId);
        boolean canAccessChild = child.canAccess(requesterUserId);

        if (!isCreator && !canAccessChild) {
            throw new AccessDeniedException("해당 세션 종료 권한이 없습니다.");
        }

        session.terminate();
    }

    /**
     */
    @Transactional
    public void terminateAllSessionsByChild(UUID childId, UUID userId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동을 찾을 수 없습니다."));

        if (!child.isPrimaryParent(userId)) {
            throw new AccessDeniedException("주 보호자만 아동 세션을 모두 종료할 수 있습니다.");
        }

        sessionRepository.terminateAllSessionsByChildId(childId);
    }

    /**
     * 만료된 세션 정리
     */
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
        sessionRepository.deleteExpiredSessions(cutoffTime);
    }
}
