package com.planB.myexpressionfriend.common.dto.note;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

/**
 * 노트 수정 요청 DTO
 */
@Getter
@Builder
public class ChildNoteUpdateDTO {

    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    private String title;

    @Size(min = 1, max = 50000, message = "본문은 1-50,000자 사이여야 합니다")
    private String content;
}