package com.planB.myexpressionfriend.common.dto.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Note comment create request DTO
 */
@Getter
@Builder
public class NoteCommentCreateDTO {

    @NotNull(message = "noteId is required.")
    private UUID noteId;

    private UUID parentCommentId;

    @NotBlank(message = "content is required.")
    @Size(max = 5000, message = "content must be <= 5000 characters.")
    private String content;
}

