package com.planB.myexpressionfriend.common.dto.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

/**
 * Note comment update request DTO
 */
@Getter
@Builder
public class NoteCommentUpdateDTO {

    @NotBlank(message = "content is required.")
    @Size(max = 5000, message = "content must be <= 5000 characters.")
    private String content;
}

