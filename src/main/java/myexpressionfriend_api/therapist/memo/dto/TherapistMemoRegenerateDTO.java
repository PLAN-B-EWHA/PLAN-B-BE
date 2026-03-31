package myexpressionfriend_api.therapist.memo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TherapistMemoRegenerateDTO {

    /** 재생성 시 LLM 프롬프트에 포함할 치료사 피드백 (선택) */
    private String therapistFeedback;
}
