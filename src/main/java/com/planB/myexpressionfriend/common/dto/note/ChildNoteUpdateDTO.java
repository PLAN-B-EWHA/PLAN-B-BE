package com.planB.myexpressionfriend.common.dto.note;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

/**
 * Child note update request DTO
 */
@Getter
@Builder
public class ChildNoteUpdateDTO {

    @Size(max = 200, message = "title must be <= 200 characters.")
    private String title;

    @Size(min = 1, max = 50000, message = "content must be between 1 and 50000 characters.")
    private String content;
}

