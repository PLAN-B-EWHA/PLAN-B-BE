package myexpressionfriend_api.game.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.common.exception.InvalidRequestException;
import myexpressionfriend_api.game.domain.*;
import myexpressionfriend_api.game.dto.DialogueResultSaveRequestDTO;
import myexpressionfriend_api.game.dto.ExpressionResultSaveRequestDTO;
import myexpressionfriend_api.game.repository.ChildScenarioProgressRepository;
import myexpressionfriend_api.game.repository.DialogueSessionRepository;
import myexpressionfriend_api.game.repository.ExpressionSessionRepository;
import myexpressionfriend_api.player.service.GamePlayerSelectionService;
import myexpressionfriend_api.scenario.domain.DialogueOption;
import myexpressionfriend_api.scenario.domain.Scenario;
import myexpressionfriend_api.scenario.domain.ScenarioApprovalStatus;
import myexpressionfriend_api.scenario.domain.ScenarioDialogueTurn;
import myexpressionfriend_api.scenario.repository.ScenarioRepository;
import myexpressionfriend_api.statistics.dialogue.service.DialogueStatisticsService;
import myexpressionfriend_api.statistics.expression.service.ExpressionStatisticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameResultService {

    private final DialogueSessionRepository dialogueSessionRepository;
    private final ExpressionSessionRepository expressionSessionRepository;
    private final ChildScenarioProgressRepository childScenarioProgressRepository;
    private final GamePlayerSelectionService gamePlayerSelectionService;
    private final ScenarioRepository scenarioRepository;
    private final DialogueStatisticsService dialogueStatisticsService;
    private final ExpressionStatisticsService expressionStatisticsService;
    private final ExpressionEmotionValidator expressionEmotionValidator;

    @Transactional
    public UUID saveDialogueResult(UUID userId, DialogueResultSaveRequestDTO dto) {
        log.info("[game-result][dialogue] step=selected-child start userId={}", userId);
        Child child = gamePlayerSelectionService.getSelectedPlayableChild(userId);
        log.info("[game-result][dialogue] step=selected-child success userId={}, childId={}", userId, child.getChildId());

        log.info("[game-result][dialogue] step=scenario-validation start scenarioId={}", dto.scenarioId());
        Scenario scenario = findPublishedServerScenario(dto.scenarioId());
        log.info("[game-result][dialogue] step=scenario-validation success scenarioId={}, source={}, status={}",
                dto.scenarioId(), scenario.getSource(), scenario.getApprovalStatus());

        float scoreRate = dto.maxScore() > 0
                ? (float) dto.totalScore() / dto.maxScore()
                : 0f;
        log.info("[game-result][dialogue] step=score-calculated childId={}, scenarioId={}, totalScore={}, maxScore={}, scoreRate={}",
                child.getChildId(), dto.scenarioId(), dto.totalScore(), dto.maxScore(), scoreRate);

        // Build lookup map: turnOrder -> optionOrder -> reactionExpression
        log.info("[game-result][dialogue] step=reaction-map start scenarioId={}", dto.scenarioId());
        Map<Integer, Map<Integer, String>> reactionMap = buildReactionMap(dto.scenarioId());
        int reactionOptionCount = reactionMap.values().stream().mapToInt(Map::size).sum();
        log.info("[game-result][dialogue] step=reaction-map success scenarioId={}, turnKeys={}, reactionOptions={}",
                dto.scenarioId(), reactionMap.size(), reactionOptionCount);

        DialogueSession session = DialogueSession.builder()
                .child(child)
                .scenarioId(dto.scenarioId())
                .scenarioSource(scenario.getSource())
                .theme(dto.theme())
                .totalScore(dto.totalScore())
                .maxScore(dto.maxScore())
                .scoreRate(scoreRate)
                .startedAt(dto.startedAt())
                .endedAt(dto.endedAt())
                .build();
        log.info("[game-result][dialogue] step=session-built childId={}, scenarioId={}, theme={}, startedAt={}, endedAt={}",
                child.getChildId(), dto.scenarioId(), dto.theme(), dto.startedAt(), dto.endedAt());

        log.info("[game-result][dialogue] step=turns-build start turnCount={}", dto.turns().size());
        for (DialogueResultSaveRequestDTO.TurnDTO turnDto : dto.turns()) {
            String reactionExpression = Optional.ofNullable(reactionMap.get(turnDto.turnNumber()))
                    .map(optMap -> optMap.get(turnDto.selectedOptionOrder()))
                    .orElse(null);

            session.getTurns().add(DialogueTurn.builder()
                    .session(session)
                    .child(child)
                    .turnNumber(turnDto.turnNumber())
                    .selectedOptionOrder(turnDto.selectedOptionOrder())
                    .selectedScore(turnDto.selectedScore())
                    .npcReactionExpression(reactionExpression)
                    .build());
        }

        log.info("[game-result][dialogue] step=turns-build success builtTurns={}", session.getTurns().size());

        log.info("[game-result][dialogue] step=session-save start childId={}, scenarioId={}", child.getChildId(), dto.scenarioId());
        DialogueSession savedSession = dialogueSessionRepository.save(session);
        log.info("[game-result][dialogue] step=session-save success sessionId={}", savedSession.getSessionId());
        log.info("[game-result][dialogue] step=scenario-progress start childId={}, scenarioId={}, sessionId={}",
                child.getChildId(), savedSession.getScenarioId(), savedSession.getSessionId());
        markScenarioCompleted(child, savedSession);
        log.info("[game-result][dialogue] step=scenario-progress done childId={}, scenarioId={}, sessionId={}",
                child.getChildId(), savedSession.getScenarioId(), savedSession.getSessionId());
        log.info("[game-result][dialogue] step=statistics-upsert start childId={}, sessionId={}",
                child.getChildId(), savedSession.getSessionId());
        dialogueStatisticsService.upsertForSession(child.getChildId(), savedSession);
        log.info("[game-result][dialogue] step=statistics-upsert success childId={}, sessionId={}",
                child.getChildId(), savedSession.getSessionId());
        log.info("[game-result][dialogue] completed userId={}, childId={}, sessionId={}",
                userId, child.getChildId(), savedSession.getSessionId());
        log.info("dialogue 결과 저장!");
        return savedSession.getSessionId();
    }

    @Transactional
    public UUID saveExpressionResult(UUID userId, ExpressionResultSaveRequestDTO dto) {
        log.info("[game-result][expression] step=selected-child start userId={}", userId);
        Child child = gamePlayerSelectionService.getSelectedPlayableChild(userId);
        log.info("[game-result][expression] step=selected-child success userId={}, childId={}", userId, child.getChildId());

        log.info("[game-result][expression] step=emotion-validation start rawEmotionTarget={}", dto.emotionTarget());
        String emotionTarget = expressionEmotionValidator.normalizeAndValidate(dto.emotionTarget());
        log.info("[game-result][expression] step=emotion-validation success rawEmotionTarget={}, normalizedEmotionTarget={}",
                dto.emotionTarget(), emotionTarget);

        ExpressionSession session = ExpressionSession.builder()
                .child(child)
                .emotionTarget(emotionTarget)
                .finalAccuracy(dto.finalAccuracy())
                .isSuccess(dto.isSuccess())
                .totalTries(dto.tries().size())
                .startedAt(dto.startedAt())
                .endedAt(dto.endedAt())
                .build();
        log.info("[game-result][expression] step=session-built childId={}, emotionTarget={}, finalAccuracy={}, isSuccess={}, totalTries={}, startedAt={}, endedAt={}",
                child.getChildId(), emotionTarget, dto.finalAccuracy(), dto.isSuccess(), dto.tries().size(), dto.startedAt(), dto.endedAt());

        log.info("[game-result][expression] step=tries-build start tryCount={}", dto.tries().size());
        for (ExpressionResultSaveRequestDTO.TryDTO tryDto : dto.tries()) {
            session.getTries().add(ExpressionTry.builder()
                    .session(session)
                    .child(child)
                    .tryNumber(tryDto.tryNumber())
                    .accuracyScore(tryDto.accuracyScore())
                    .durationMs(tryDto.durationMs())
                    .isSuccess(tryDto.isSuccess())
                    .build());
        }

        log.info("[game-result][expression] step=tries-build success builtTries={}", session.getTries().size());

        log.info("[game-result][expression] step=session-save start childId={}, emotionTarget={}", child.getChildId(), emotionTarget);
        ExpressionSession savedSession = expressionSessionRepository.save(session);
        log.info("[game-result][expression] step=session-save success sessionId={}", savedSession.getSessionId());

        log.info("[game-result][expression] step=statistics-upsert start childId={}, sessionId={}",
                child.getChildId(), savedSession.getSessionId());
        expressionStatisticsService.upsertForSession(child.getChildId(), savedSession);
        log.info("[game-result][expression] step=statistics-upsert success childId={}, sessionId={}",
                child.getChildId(), savedSession.getSessionId());
        log.info("[game-result][expression] completed userId={}, childId={}, sessionId={}",
                userId, child.getChildId(), savedSession.getSessionId());
        log.info("Expression 결과 저장!");
        return savedSession.getSessionId();
    }

    /**
     * Build a nested map of turnOrder -> (optionOrder -> reactionExpression)
     * from the scenario. Returns empty map if scenario not found.
     */
    private Map<Integer, Map<Integer, String>> buildReactionMap(String scenarioId) {
        return scenarioRepository.findWithFullDetailAndOptions(scenarioId)
                .map(Scenario::getDialogueFlow)
                .map(turns -> turns.stream().collect(Collectors.toMap(
                        ScenarioDialogueTurn::getTurnOrder,
                        turn -> turn.getOptions().stream()
                                .filter(opt -> opt.getReactionExpression() != null)
                                .collect(Collectors.toMap(
                                        DialogueOption::getOptionOrder,
                                        DialogueOption::getReactionExpression,
                                        (a, b) -> a
                                ))
                )))
                .orElse(Map.of());
    }

    private Scenario findPublishedServerScenario(String scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new InvalidRequestException(
                        "배포된 서버 시나리오만 결과를 저장할 수 있습니다. scenario_id=" + scenarioId));

        boolean publishedServerScenario = scenario.getApprovalStatus() == ScenarioApprovalStatus.PUBLISHED
                && scenario.getSource() != ScenarioSource.UNITY_LOCAL;
        if (!publishedServerScenario) {
            throw new InvalidRequestException(
                    "배포된 서버 시나리오만 결과를 저장할 수 있습니다. scenario_id=" + scenarioId);
        }
        return scenario;
    }

    private void markScenarioCompleted(Child child, DialogueSession session) {
        boolean alreadyCompleted = childScenarioProgressRepository
                .findByChild_ChildIdAndScenarioId(child.getChildId(), session.getScenarioId())
                .isPresent();
        if (alreadyCompleted) {
            log.info("[game-result][dialogue] step=scenario-progress skipped childId={}, scenarioId={}, reason=already-completed",
                    child.getChildId(), session.getScenarioId());
            return;
        }

        childScenarioProgressRepository.save(ChildScenarioProgress.builder()
                .child(child)
                .scenarioId(session.getScenarioId())
                .completedAt(LocalDateTime.now())
                .completedSessionId(session.getSessionId())
                .build());
        log.info("[game-result][dialogue] step=scenario-progress inserted childId={}, scenarioId={}, completedSessionId={}",
                child.getChildId(), session.getScenarioId(), session.getSessionId());
    }
}
