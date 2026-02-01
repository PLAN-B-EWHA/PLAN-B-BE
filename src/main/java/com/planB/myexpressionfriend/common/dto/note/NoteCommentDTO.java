package com.planB.myexpressionfriend.common.dto.note;

import com.planB.myexpressionfriend.common.domain.note.NoteComment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 댓글 응답 DTO
 * 대댓글 포함 (계층 구조)
 */
@Getter
@Builder
public class NoteCommentDTO {

    private UUID commentId;
    private UUID noteId;
    private UUID authorId;
    private String authorName;
    private UUID parentCommentId; // 대댓글인 경우 부모 댓글 ID
    private String content;
    private List<NoteCommentDTO> replies; // 대댓글 목록
    private int replyCount; // 대댓글 개수
    private boolean isTopLevel; // 최상위 댓글 여부
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity -> DTO 변환
     */
    public static NoteCommentDTO from(NoteComment comment) {
        return NoteCommentDTO.builder()
                .commentId(comment.getCommentId())
                .noteId(comment.getNote().getNoteId())
                .authorId(comment.getAuthor().getUserId())
                .authorName(comment.getAuthor().getName())
                .parentCommentId(comment.getParentComment() != null
                        ? comment.getParentComment().getCommentId()
                        : null)
                .content(comment.getContent())
                .replies(comment.getReplies().stream()
                        .map(NoteCommentDTO::from)
                        .collect(Collectors.toList()))
                .replyCount(comment.getReplyCount())
                .isTopLevel(comment.isTopLevel())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    /**
     * Entity -> DTO 변환 (대댓글 제외)
     * 목록 조회 시 사용 (대댓글은 별도 API로 조회)
     */
    public static NoteCommentDTO fromWithoutReplies(NoteComment comment) {
        return NoteCommentDTO.builder()
                .commentId(comment.getCommentId())
                .noteId(comment.getNote().getNoteId())
                .authorId(comment.getAuthor().getUserId())
                .authorName(comment.getAuthor().getName())
                .parentCommentId(comment.getParentComment() != null
                        ? comment.getParentComment().getCommentId()
                        : null)
                .content(comment.getContent())
                .replies(List.of()) // 빈 리스트
                .replyCount(comment.getReplyCount())
                .isTopLevel(comment.isTopLevel())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}