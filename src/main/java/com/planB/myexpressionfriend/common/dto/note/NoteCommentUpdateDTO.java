package com.planB.myexpressionfriend.common.dto.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

/**
 * 댓글 수정 요청 DTO
 */
@Getter
@Builder
public class NoteCommentUpdateDTO {

    @NotBlank(message = "댓글 내용은 필수입니다")
    @Size(max = 5000, message = "댓글은 5,000자를 초과할 수 없습니다")
    private String content;
}