package com.planB.myexpressionfriend.unity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planB.myexpressionfriend.common.exception.InvalidRequestException;
import com.planB.myexpressionfriend.unity.dto.UnityMissionGenerationType;
import com.planB.myexpressionfriend.unity.dto.UnityMissionImportRequestDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnityMissionLlmParsingServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final UnityMissionLlmParsingService parsingService =
            new UnityMissionLlmParsingService(objectMapper, validator);

    @Test
    @DisplayName("표정 미션 응답을 파싱한다")
    void parseMissionBatch_withExpressionPayload_returnsRequestDto() {
        String rawJson = """
                ```json
                {
                  "missions": [
                    {
                      "missionId": 1,
                      "missionName": "슬픈 표정 연습",
                      "missionTypeString": "Expression",
                      "targetKeyword": "Sad",
                      "targetEmotionString": "Sadness",
                      "expression_data": {
                        "characterDialogue": ["속상한 일이 있어.", "슬픈 표정 지어봐."],
                        "successFeedback": ["정말 잘했어.", "마음이 보여."],
                        "retryFeedback": ["괜찮아.", "한 번 더 해볼까?"],
                        "failFeedback": ["오늘은 여기까지.", "내일 다시 해보자."]
                      }
                    }
                  ]
                }
                ```
                """;

        UnityMissionImportRequestDTO result =
                parsingService.parseMissionBatch(
                        rawJson,
                        UnityMissionGenerationType.EXPRESSION,
                        java.util.Set.of("Sadness")
                );

        assertNotNull(result);
        assertEquals(1, result.getMissions().size());
        assertEquals("Expression", result.getMissions().get(0).getMissionTypeString());
        assertNotNull(result.getMissions().get(0).getExpressionData());
    }

    @Test
    @DisplayName("허용되지 않은 감정 값이면 예외가 발생한다")
    void parseMissionBatch_withInvalidEmotion_throwsException() {
        String rawJson = """
                {
                  "missions": [
                    {
                      "missionId": 1,
                      "missionName": "기쁜 표정 연습",
                      "missionTypeString": "Expression",
                      "targetKeyword": "Happy",
                      "targetEmotionString": "Joy",
                      "expression_data": {
                        "characterDialogue": ["좋은 일이 있어.", "활짝 웃어보자."],
                        "successFeedback": ["잘했어.", "웃음이 보여."],
                        "retryFeedback": ["괜찮아.", "다시 해볼까?"],
                        "failFeedback": ["오늘은 여기까지.", "내일 다시 하자."]
                      }
                    }
                  ]
                }
                """;

        assertThrows(
                InvalidRequestException.class,
                () -> parsingService.parseMissionBatch(
                        rawJson,
                        UnityMissionGenerationType.EXPRESSION,
                        java.util.Set.of("Happiness")
                )
        );
    }
}
