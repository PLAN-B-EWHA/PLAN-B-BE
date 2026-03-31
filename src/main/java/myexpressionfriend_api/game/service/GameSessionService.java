package myexpressionfriend_api.game.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.auth.domain.user.User;
import myexpressionfriend_api.auth.repository.UserRepository;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.domain.ChildPermissionType;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import myexpressionfriend_api.common.exception.InvalidRequestException;
import myexpressionfriend_api.game.domain.GameSession;
import myexpressionfriend_api.game.dto.GameSessionDTO;
import myexpressionfriend_api.game.repository.GameSessionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    /**
     * 게임 세션 생성
     * - 비관적 락으로 동시 세션 생성 race condition 방지
     * - 기존 활성 세션 모두 종료 후 신규 세션 생성
     */
    @Transactional
    public GameSessionDTO createSession(UUID childId, UUID userId) {
        log.info("게임 세션 생성 - childId: {}, userId: {}", childId, userId);

        Child child = childRepository.findByIdForUpdate(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        if (!child.hasPermission(userId, ChildPermissionType.PLAY_GAME)) {
            throw new AccessDeniedException("게임 플레이 권한이 없습니다.");
        }

        sessionRepository.terminateAllSessionsByChildId(childId);

        GameSession session = GameSession.create(child, user);
        GameSession saved = sessionRepository.save(session);

        log.info("게임 세션 생성 완료 - sessionId: {}", saved.getSessionId());
        return GameSessionDTO.from(saved, true);
    }

    /**
     * PIN 검증 후 게임 세션 생성 (Unity 진입 플로우)
     * - PIN 검증 → 성공 시 기존 세션 종료 → 신규 세션 반환
     * - PIN이 비활성화된 경우 바로 세션 생성
     */
    @Transactional
    public GameSessionDTO verifyPinAndCreateSession(UUID childId, UUID userId, String rawPin) {
        log.info("PIN 검증 + 세션 생성 - childId: {}, userId: {}", childId, userId);

        Child child = childRepository.findByIdForUpdate(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        if (!child.canAccess(userId)) {
            throw new AccessDeniedException("해당 아동에 대한 접근 권한이 없습니다.");
        }

        if (Boolean.TRUE.equals(child.getPinEnabled())) {
            if (rawPin == null || rawPin.isBlank()) {
                throw new InvalidRequestException("PIN이 필요합니다.");
            }
            boolean verified = child.verifyPin(rawPin, passwordEncoder);
            if (!verified) {
                throw new InvalidRequestException("PIN이 올바르지 않습니다.");
            }
        }

        sessionRepository.terminateAllSessionsByChildId(childId);
        GameSession session = GameSession.create(child, user);
        GameSession saved = sessionRepository.save(session);

        log.info("PIN 검증 후 세션 생성 완료 - sessionId: {}", saved.getSessionId());
        return GameSessionDTO.from(saved, true);
    }

    /** 세션 토큰 유효성 검증 */
    public GameSessionDTO validateSession(String sessionToken) {
        GameSession session = sessionRepository.findValidSessionByToken(sessionToken, LocalDateTime.now())
                .orElseThrow(() -> new AccessDeniedException("유효하지 않은 세션입니다."));
        return GameSessionDTO.from(session, false);
    }

    /** 세션 lastUsedAt 갱신 */
    @Transactional
    public void refreshSession(String sessionToken) {
        GameSession session = sessionRepository.findValidSessionByToken(sessionToken, LocalDateTime.now())
                .orElseThrow(() -> new AccessDeniedException("유효하지 않은 세션입니다."));
        session.refresh();
    }

    /** 아동의 활성 세션 목록 */
    public List<GameSessionDTO> getActiveSessionsByChild(UUID childId, UUID userId) {
        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동을 찾을 수 없습니다."));

        if (!child.canAccess(userId)) {
            throw new AccessDeniedException("해당 아동에 대한 접근 권한이 없습니다.");
        }

        return sessionRepository.findActiveSessionsByChildId(childId, LocalDateTime.now()).stream()
                .map(session -> GameSessionDTO.from(session, false))
                .collect(Collectors.toList());
    }

    /** 세션 종료 */
    @Transactional
    public void terminateSession(String sessionToken, UUID requesterUserId) {
        GameSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new EntityNotFoundException("세션을 찾을 수 없습니다."));

        Child child = session.getChild();
        boolean isCreator = session.getAuthenticatedBy().getUserId().equals(requesterUserId);
        boolean canAccess = child.canAccess(requesterUserId);

        if (!isCreator && !canAccess) {
            throw new AccessDeniedException("해당 세션 종료 권한이 없습니다.");
        }

        session.terminate();
    }

    /** 아동의 모든 활성 세션 종료 (주보호자만) */
    @Transactional
    public void terminateAllSessionsByChild(UUID childId, UUID userId) {
        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동을 찾을 수 없습니다."));

        if (!child.isPrimaryParent(userId)) {
            throw new AccessDeniedException("주보호자만 아동 세션을 모두 종료할 수 있습니다.");
        }

        sessionRepository.terminateAllSessionsByChildId(childId);
    }

    /** 만료된 세션 정리 (배치용) */
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
        sessionRepository.deleteExpiredSessions(cutoffTime);
        log.info("만료 세션 정리 완료 - cutoff: {}", cutoffTime);
    }
}
