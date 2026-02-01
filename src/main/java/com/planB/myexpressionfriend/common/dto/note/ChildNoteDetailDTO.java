package com.planB.myexpressionfriend.common.dto.note;

import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.note.NoteType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 노트 상세 응답 DTO
 * 상세 조회용 (전체 본문 + 첨부파일 + 댓글 포함)
 */
@Getter
@Builder
public class ChildNoteDetailDTO {

    private UUID noteId;
    private UUID childId;
    private String childName;
    private UUID authorId;
    private String authorName;
    private NoteType type;
    private String typeDisplayName;
    private String title;
    private String content; // 전체 본문
    private List<NoteAssetDTO> assets; // 첨부파일 목록
    private List<NoteCommentDTO> comments; // 최상위 댓글 목록
    private int totalCommentCount; // 총 댓글 수 (대댓글 포함)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity -> DTO 변환
     */
    public static ChildNoteDetailDTO from(ChildNote note) {
        // 최상위 댓글만 필터링
        List<NoteCommentDTO> topLevelComments = note.getComments().stream()
                .filter(comment -> comment.getParentComment() == null)
                .map(NoteCommentDTO::from)
                .collect(Collectors.toList());

        return ChildNoteDetailDTO.builder()
                .noteId(note.getNoteId())
                .childId(note.getChild().getChildId())
                .childName(note.getChild().getName())
                .authorId(note.getAuthor().getUserId())
                .authorName(note.getAuthor().getName())
                .type(note.getType())
                .typeDisplayName(note.getType().getDisplayName())
                .title(note.getTitle())
                .content(note.getContent())
                .assets(note.getAssets().stream()
                        .map(NoteAssetDTO::from)
                        .collect(Collectors.toList()))
                .comments(topLevelComments)
                .totalCommentCount(note.getComments().size())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}