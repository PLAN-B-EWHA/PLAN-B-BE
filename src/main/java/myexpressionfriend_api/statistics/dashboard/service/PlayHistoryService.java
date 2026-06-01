package myexpressionfriend_api.statistics.dashboard.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.common.domain.PeersTheme;
import myexpressionfriend_api.common.exception.AuthenticationFailedException;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import myexpressionfriend_api.game.domain.DialogueSession;
import myexpressionfriend_api.game.domain.DialogueTurn;
import myexpressionfriend_api.game.domain.ExpressionSession;
import myexpressionfriend_api.game.domain.ExpressionTry;
import myexpressionfriend_api.game.dto.history.DialogueHistoryDto;
import myexpressionfriend_api.game.dto.history.ExpressionHistoryDto;
import myexpressionfriend_api.game.repository.DialogueSessionRepository;
import myexpressionfriend_api.game.repository.DialogueTurnRepository;
import myexpressionfriend_api.game.repository.ExpressionSessionRepository;
import myexpressionfriend_api.game.repository.ExpressionTryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayHistoryService {

    private final ChildRepository childRepository;
    private final DialogueSessionRepository dialogueSessionRepository;
    private final DialogueTurnRepository dialogueTurnRepository;
    private final ExpressionSessionRepository expressionSessionRepository;
    private final ExpressionTryRepository expressionTryRepository;

    @Transactional(readOnly = true)
    public Page<DialogueHistoryDto> getDialogueHistory(
            UUID userId, UUID childId, String theme, Pageable pageable) {

        checkPermission(userId, childId);

        Page<DialogueSession> sessionPage = (theme != null && !theme.isBlank())
                ? dialogueSessionRepository.findByChild_ChildIdAndThemeOrderByStartedAtDesc(
                childId, PeersTheme.fromDisplayName(theme), pageable)
                : dialogueSessionRepository.findByChild_ChildIdOrderByStartedAtDesc(childId, pageable);

        if (sessionPage.isEmpty()) return sessionPage.map(s -> null);

        List<UUID> sessionIds = sessionPage.map(DialogueSession::getSessionId).toList();
        Map<UUID, List<DialogueTurn>> turnsBySession = dialogueTurnRepository
                .findBySession_SessionIdInOrderByTurnNumber(sessionIds)
                .stream()
                .collect(Collectors.groupingBy(t -> t.getSession().getSessionId()));

        return sessionPage.map(s -> {
            List<DialogueHistoryDto.TurnDto> turns = turnsBySession
                    .getOrDefault(s.getSessionId(), List.of())
                    .stream()
                    .map(t -> new DialogueHistoryDto.TurnDto(
                            t.getTurnNumber(),
                            t.getSelectedOptionOrder(),
                            t.getSelectedScore(),
                            t.getNpcReactionExpression()
                    ))
                    .toList();

            return new DialogueHistoryDto(
                    s.getSessionId(),
                    s.getTheme().getDisplayName(),
                    s.getScenarioId(),
                    s.getScenarioSource(),
                    s.getStartedAt(),
                    ChronoUnit.SECONDS.between(s.getStartedAt(), s.getEndedAt()),
                    s.getTotalScore(),
                    s.getMaxScore(),
                    s.getScoreRate(),
                    turns
            );
        });
    }

    @Transactional(readOnly = true)
    public Page<ExpressionHistoryDto> getExpressionHistory(
            UUID userId, UUID childId, String emotion, Pageable pageable) {

        checkPermission(userId, childId);

        Page<ExpressionSession> sessionPage = (emotion != null && !emotion.isBlank())
                ? expressionSessionRepository.findByChild_ChildIdAndEmotionTargetOrderByStartedAtDesc(
                childId, emotion, pageable)
                : expressionSessionRepository.findByChild_ChildIdOrderByStartedAtDesc(childId, pageable);

        if (sessionPage.isEmpty()) return sessionPage.map(s -> null);

        List<UUID> sessionIds = sessionPage.map(ExpressionSession::getSessionId).toList();
        Map<UUID, List<ExpressionTry>> triesBySession = expressionTryRepository
                .findBySession_SessionIdInOrderByTryNumber(sessionIds)
                .stream()
                .collect(Collectors.groupingBy(t -> t.getSession().getSessionId()));

        return sessionPage.map(s -> {
            List<ExpressionHistoryDto.TryDto> tries = triesBySession
                    .getOrDefault(s.getSessionId(), List.of())
                    .stream()
                    .map(t -> new ExpressionHistoryDto.TryDto(
                            t.getTryNumber(),
                            t.getAccuracyScore(),
                            t.getDurationMs(),
                            t.getIsSuccess()
                    ))
                    .toList();

            return new ExpressionHistoryDto(
                    s.getSessionId(),
                    s.getEmotionTarget(),
                    s.getStartedAt(),
                    s.getFinalAccuracy(),
                    s.getIsSuccess(),
                    s.getTotalTries(),
                    tries
            );
        });
    }

    private void checkPermission(UUID userId, UUID childId) {
        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동 정보를 찾을 수 없습니다."));
        if (!child.canAccess(userId)) {
            throw new AuthenticationFailedException("해당 아동에 대한 접근 권한이 없습니다.");
        }
    }
}
