package myexpressionfriend_api.therapist.memo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class TherapistMemoCreateDTO {

    @NotNull(message = "주차(week_of)는 필수입니다.")
    private LocalDate weekOf;

    @NotBlank(message = "세션 내용은 필수입니다.")
    private String content;

    /** 생성 즉시 보호자 공개 여부 (기본값 false) */
    private boolean isVisibleToParent = false;
}
