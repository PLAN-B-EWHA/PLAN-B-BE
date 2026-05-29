package myexpressionfriend_api.scenario.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import myexpressionfriend_api.game.domain.ScenarioSource;
import myexpressionfriend_api.scenario.domain.Scenario;
import myexpressionfriend_api.scenario.domain.ScenarioApprovalStatus;

import java.util.List;

/**
 * 시나리오 공용 DTO — Import(입력) / Response(출력) 모두 사용.
 * JSON 키는 기존 시나리오 파일 형식을 그대로 유지합니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScenarioDTO(
        @JsonProperty("scenario_id")   String scenarioId,
        @JsonProperty("is_completed") Boolean isCompleted,
        ScenarioSource source,
        @JsonProperty("approval_status") ScenarioApprovalStatus approvalStatus,
        Metadata metadata,
        Cast cast,
        @JsonProperty("dialogue_flow") List<DialogueTurnDTO> dialogueFlow,
        @JsonProperty("final_summary") FinalSummary finalSummary
) {

    // ── 중첩 레코드 ─────────────────────────────────────────────────────

    public record Metadata(
            Integer week,
            String theme,
            @JsonProperty("relationship_stage") String relationshipStage,
            @JsonProperty("scenario_seed")      String scenarioSeed,
            @JsonProperty("lobby_title")        String lobbyTitle,
            @JsonProperty("background_image_id") String backgroundImageId
    ) {}

    public record Cast(
            @JsonProperty("main_character") String mainCharacter,
            @JsonProperty("main_char_pos")  String mainCharPos,
            @JsonProperty("sub_characters") String subCharacters,
            @JsonProperty("sub_char_pos")   String subCharPos
    ) {}

    public record FinalSummary(
            @JsonProperty("total_learning_point") String totalLearningPoint
    ) {}

    // ── 팩토리 ─────────────────────────────────────────────────────────

    public static ScenarioDTO from(Scenario s) {
        return from(s, null);
    }

    public static ScenarioDTO from(Scenario s, Boolean isCompleted) {
        Metadata meta = new Metadata(
                s.getWeek(), s.getTheme(), s.getRelationshipStage(),
                s.getScenarioSeed(), s.getLobbyTitle(), s.getBackgroundImageId()
        );
        Cast cast = new Cast(
                s.getMainCharacter(), s.getMainCharPos(),
                s.getSubCharacters(), s.getSubCharPos()
        );
        FinalSummary summary = s.getFinalLearningPoint() != null
                ? new FinalSummary(s.getFinalLearningPoint()) : null;

        List<DialogueTurnDTO> turns = s.getDialogueFlow().stream()
                .map(DialogueTurnDTO::from)
                .toList();

        return new ScenarioDTO(
                s.getScenarioId(),
                isCompleted,
                s.getSource(),
                s.getApprovalStatus(),
                meta,
                cast,
                turns,
                summary
        );
    }
}
