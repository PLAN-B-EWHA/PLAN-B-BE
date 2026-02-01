package com.planB.myexpressionfriend.common.dto.note;

import com.planB.myexpressionfriend.common.domain.note.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * 노트 생성 요청 DTO
 */
@Getter
@Builder
public class ChildNoteCreateDTO {

    @NotNull(message = "아동 ID는 필수입니다")
    private UUID childId;

    @NotNull(message = "노트 타입은 필수입니다")
    private NoteType type;

    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    private String title;

    @NotBlank(message = "본문은 필수입니다")
    @Size(max = 50000, message = "본문은 50,000자를 초과할 수 없습니다")
    private String content;

    /**
     * SYSTEM 타입 노트는 사용자가 직접 생성할 수 없음
     */
    public void validateUserWritable() {
        if (type != null && !type.isUserWritable()) {
            throw new IllegalArgumentException("SYSTEM 타입 노트는 직접 생성할 수 없습니다");
        }
    }
}