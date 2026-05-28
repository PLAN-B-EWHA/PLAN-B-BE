package myexpressionfriend_api.admin.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.admin.dto.AdminGameRecordSeedRequestDTO;
import myexpressionfriend_api.admin.dto.AdminGameRecordSeedResponseDTO;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.common.domain.PeersTheme;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import myexpressionfriend_api.game.domain.DialogueSession;
import myexpressionfriend_api.game.domain.DialogueTurn;
import myexpressionfriend_api.game.domain.ExpressionSession;
import myexpressionfriend_api.game.domain.ExpressionTry;
import myexpressionfriend_api.game.domain.ScenarioSource;
import myexpressionfriend_api.game.repository.DialogueSessionRepository;
import myexpressionfriend_api.game.repository.ExpressionSessionRepository;
import myexpressionfriend_api.game.service.ExpressionEmotionValidator;
import myexpressionfriend_api.statistics.dialogue.service.DialogueStatisticsService;
import myexpressionfriend_api.statistics.expression.service.ExpressionStatisticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminGameRecordSeedService {

    private final ChildRepository childRepository;
    private final DialogueSessionRepository dialogueSessionRepository;
    private final ExpressionSessionRepository expressionSessionRepository;
    private final DialogueStatisticsService dialogueStatisticsService;
    private final ExpressionStatisticsService expressionStatisticsService;
    private final ExpressionEmotionValidator expressionEmotionValidator;

    @Transactional
    public AdminGameRecordSeedResponseDTO seed(AdminGameRecordSeedRequestDTO request) {
        Child child = childRepository.findById(request.childId())
                .orElseThrow(() -> new EntityNotFoundException("Child not found. id=" + request.childId()));

        PeersTheme theme = request.themeOrDefault();
        String emotion = expressionEmotionValidator.normalizeAndValidate(request.emotionTargetOrDefault());
        int dialogueSessionCount = request.dialogueSessionCountOrDefault();
        int expressionSessionCount = request.expressionSessionCountOrDefault();
        LocalDate startDate = request.startDateOrDefault(Math.max(dialogueSessionCount, expressionSessionCount));

        List<UUID> dialogueIds = new ArrayList<>();
        List<UUID> expressionIds = new ArrayList<>();

        for (int i = 0; i < dialogueSessionCount; i++) {
            DialogueSession session = createDialogueSession(child, theme, startDate, i);
            DialogueSession saved = dialogueSessionRepository.save(session);
            dialogueStatisticsService.upsertForSession(child.getChildId(), saved);
            dialogueIds.add(saved.getSessionId());
        }

        for (int i = 0; i < expressionSessionCount; i++) {
            ExpressionSession session = createExpressionSession(child, emotion, startDate, i);
            ExpressionSession saved = expressionSessionRepository.save(session);
            expressionStatisticsService.upsertForSession(child.getChildId(), saved);
            expressionIds.add(saved.getSessionId());
        }

        return new AdminGameRecordSeedResponseDTO(
                child.getChildId(),
                dialogueIds.size(),
                expressionIds.size(),
                dialogueIds,
                expressionIds
        );
    }

    private DialogueSession createDialogueSession(Child child, PeersTheme theme, LocalDate startDate, int index) {
        Instant startedAt = toInstant(startDate.plusDays(index), 16, 0);
        Instant endedAt = startedAt.plusSeconds(Math.max(80, 170 - (long) index * 8));
        int[] scores = dialogueScores(index);
        int totalScore = scores[0] + scores[1] + scores[2];
        int maxScore = 6;

        DialogueSession session = DialogueSession.builder()
                .child(child)
                .scenarioId("ADMIN_TEST_" + theme.name() + "_" + (index + 1))
                .scenarioSource(ScenarioSource.UNITY_LOCAL)
                .theme(theme)
                .totalScore(totalScore)
                .maxScore(maxScore)
                .scoreRate((float) totalScore / maxScore)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .build();

        for (int turn = 1; turn <= 3; turn++) {
            int score = scores[turn - 1];
            session.getTurns().add(DialogueTurn.builder()
                    .session(session)
                    .child(child)
                    .turnNumber(turn)
                    .selectedOptionOrder(score == 2 ? 0 : score == 1 ? 1 : 2)
                    .selectedScore(score)
                    .npcReactionExpression(score == 2 ? "Joy" : score == 1 ? "Neutral" : "Anger")
                    .build());
        }
        return session;
    }

    private ExpressionSession createExpressionSession(Child child, String emotion, LocalDate startDate, int index) {
        Instant startedAt = toInstant(startDate.plusDays(index), 17, 0);
        Instant endedAt = startedAt.plusSeconds(Math.max(45, 110 - (long) index * 7));
        int totalTries = Math.max(1, 4 - Math.min(index, 3));
        float finalAccuracy = Math.min(0.95f, 0.55f + index * 0.08f);

        ExpressionSession session = ExpressionSession.builder()
                .child(child)
                .emotionTarget(emotion)
                .finalAccuracy(finalAccuracy)
                .isSuccess(finalAccuracy >= 0.7f)
                .totalTries(totalTries)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .build();

        for (int tryNumber = 1; tryNumber <= totalTries; tryNumber++) {
            float accuracy = Math.min(finalAccuracy, 0.45f + index * 0.08f + tryNumber * 0.05f);
            session.getTries().add(ExpressionTry.builder()
                    .session(session)
                    .child(child)
                    .tryNumber(tryNumber)
                    .accuracyScore(accuracy)
                    .durationMs(Math.max(1800, 5200 - index * 350 - tryNumber * 250))
                    .isSuccess(accuracy >= 0.7f)
                    .build());
        }
        return session;
    }

    private int[] dialogueScores(int index) {
        return switch (Math.min(index, 7)) {
            case 0 -> new int[]{0, 1, 1};
            case 1 -> new int[]{1, 1, 1};
            case 2 -> new int[]{1, 1, 2};
            case 3 -> new int[]{1, 2, 2};
            case 4 -> new int[]{2, 1, 2};
            case 5 -> new int[]{2, 2, 1};
            default -> new int[]{2, 2, 2};
        };
    }

    private Instant toInstant(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toInstant();
    }
}
