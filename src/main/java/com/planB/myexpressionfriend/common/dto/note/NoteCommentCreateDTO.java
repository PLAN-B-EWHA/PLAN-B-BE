package com.planB.myexpressionfriend.common.dto.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * 댓글 생성 요청 DTO
 */
@Getter
@Builder
public class NoteCommentCreateDTO {

    @NotNull(message = "노트 ID는 필수입니다")
    private UUID noteId;

    /**
     * 부모 댓글 ID (대댓글인 경우)
     * null이면 최상위 댓글
     */
    private UUID parentCommentId;

    @NotBlank(message = "댓글 내용은 필수입니다")
    @Size(max = 5000, message = "댓글은 5,000자를 초과할 수 없습니다")
    private String content;
}