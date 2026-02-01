package com.planB.myexpressionfriend.common.dto.note;

import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.note.NoteType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 노트 기본 응답 DTO
 * 목록 조회용 (간단한 정보만 포함)
 */
@Getter
@Builder
public class ChildNoteDTO {

    private UUID noteId;
    private UUID childId;
    private String childName;
    private UUID authorId;
    private String authorName;
    private NoteType type;
    private String typeDisplayName;
    private String title;
    private String contentPreview; // 본문 미리보기 (100자)
    private int assetCount; // 첨부파일 개수
    private int commentCount; // 댓글 개수
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity -> DTO 변환
     */
    public static ChildNoteDTO from(ChildNote note) {
        return ChildNoteDTO.builder()
                .noteId(note.getNoteId())
                .childId(note.getChild().getChildId())
                .childName(note.getChild().getName())
                .authorId(note.getAuthor().getUserId())
                .authorName(note.getAuthor().getName())
                .type(note.getType())
                .typeDisplayName(note.getType().getDisplayName())
                .title(note.getTitle())
                .contentPreview(createPreview(note.getContent()))
                .assetCount(note.getAssets().size())
                .commentCount(note.getComments().size())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }

    /**
     * 본문 미리보기 생성 (100자)
     */
    private static String createPreview(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= 100) {
            return content;
        }
        return content.substring(0, 100) + "...";
    }
}