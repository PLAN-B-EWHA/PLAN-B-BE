package com.planB.myexpressionfriend.common.dto.note;

import com.planB.myexpressionfriend.common.domain.note.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Child note create request DTO
 */
@Getter
@Builder
public class ChildNoteCreateDTO {

    @NotNull(message = "childId is required.")
    private UUID childId;

    @NotNull(message = "type is required.")
    private NoteType type;

    @Size(max = 200, message = "title must be <= 200 characters.")
    private String title;

    @NotBlank(message = "content is required.")
    @Size(max = 50000, message = "content must be <= 50000 characters.")
    private String content;

    public void validateUserWritable() {
        if (type != null && !type.isUserWritable()) {
            throw new IllegalArgumentException("SYSTEM note type cannot be created directly.");
        }
    }
}

