package myexpressionfriend_api.therapist.memo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TherapistMemoUpdateDTO {

    /** null 이면 변경하지 않음 */
    private String content;
    private String parentContent;
    private String homePracticeTip;
    private Boolean isVisibleToParent;
}
