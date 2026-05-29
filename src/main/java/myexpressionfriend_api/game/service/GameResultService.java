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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
        Child child = gamePlayerSelectionService.getSelectedPlayableChild(userId);
        Scenario scenario = findPublishedServerScenario(dto.scenarioId());

        float scoreRate = dto.maxScore() > 0
                ? (float) dto.totalScore() / dto.maxScore()
                : 0f;

        // Build lookup map: turnOrder -> optionOrder -> reactionExpression
        Map<Integer, Map<Integer, String>> reactionMap = buildReactionMap(dto.scenarioId());

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

        DialogueSession savedSession = dialogueSessionRepository.save(session);
        markScenarioCompleted(child, savedSession);
        dialogueStatisticsService.upsertForSession(child.getChildId(), savedSession);
        return savedSession.getSessionId();
    }

    @Transactional
    public UUID saveExpressionResult(UUID userId, ExpressionResultSaveRequestDTO dto) {
        Child child = gamePlayerSelectionService.getSelectedPlayableChild(userId);
        String emotionTarget = expressionEmotionValidator.normalizeAndValidate(dto.emotionTarget());

        ExpressionSession session = ExpressionSession.builder()
                .child(child)
                .emotionTarget(emotionTarget)
                .finalAccuracy(dto.finalAccuracy())
                .isSuccess(dto.isSuccess())
                .totalTries(dto.tries().size())
                .startedAt(dto.startedAt())
                .endedAt(dto.endedAt())
                .build();

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

        ExpressionSession savedSession = expressionSessionRepository.save(session);
        expressionStatisticsService.upsertForSession(child.getChildId(), savedSession);
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
            return;
        }

        childScenarioProgressRepository.save(ChildScenarioProgress.builder()
                .child(child)
                .scenarioId(session.getScenarioId())
                .completedAt(LocalDateTime.now())
                .completedSessionId(session.getSessionId())
                .build());
    }
}
