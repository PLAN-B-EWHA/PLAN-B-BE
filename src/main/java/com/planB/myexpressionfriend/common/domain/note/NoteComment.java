package com.planB.myexpressionfriend.common.domain.note;


import com.planB.myexpressionfriend.common.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 노트 댓글 엔티티
 *
 * 기능:
 * - 노트에 대한 댓글 작성
 * - 대댓글 지원 (self-referencing)
 * - 부모-치료사 간 소통 지원
 *
 * 권한:
 * - 작성: VIEW_REPORT 권한 (노트 조회 가능하면 댓글 작성 가능)
 * - 수정: 작성자 본인만
 * - 삭제: 작성자 본인 또는 주보호자
 */
@Entity
@Table(name = "note_comments", indexes = {
        @Index(name = "idx_comments_note", columnList = "note_id"),
        @Index(name = "idx_comments_author", columnList = "author_id"),
        @Index(name = "idx_comments_parent", columnList = "parent_comment_id"),
        @Index(name = "idx_comments_created", columnList = "created_at"),
        @Index(name = "idx_comments_deleted", columnList = "is_deleted")
})
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE note_comments SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE comment_id = ?")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"note", "author", "parentComment", "replies"})
@EntityListeners(AuditingEntityListener.class)
public class NoteComment {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "comment_id", updatable = false, nullable = false)
    private UUID commentId;

    /**
     * 소속 노트
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    @Setter
    private ChildNote note;

    /**
     * 작성자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * 부모 댓글 (대댓글의 경우)
     * null일시 최상위 댓글
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    @Setter
    private NoteComment parentComment;

    /**
     * 자식 댓글들(대댓글 목록)
     */
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NoteComment> replies = new ArrayList<>();

    /**
     * 댓글 내용
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Soft Delete
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ============= 비즈니스 메서드 =============

    /**
     * 댓글 내용 변경
     */
    public void changeContent(String content) {

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("댓글 내용은 필수입니다");
        }

        if (content.length() > 5000) {
            throw new IllegalArgumentException("댓글은 5,000자를 초과할 수 없습니다");
        }

        this.content = content.trim();
    }

    /**
     * 작성자 확인
     */
    public boolean isAuthor(UUID userId) {
        return this.author.getUserId().equals(userId);
    }

    /**
     * 최상위 댓글인지 확인
     */
    public boolean isTopLevel() {
        return this.parentComment == null;
    }

    /**
     * 대댓글인지 확인
     */
    public boolean isReply() {
        return this.parentComment != null;
    }

    /**
     * 수정 권한 확인
     */
    public boolean canEdit(UUID userId) {
        return isAuthor(userId);
    }

    /**
     * 삭제 권한 확인
     */
    public boolean canDelete(UUID userId) {
        return isAuthor(userId) || note.getChild().isPrimaryParent(userId);
    }

    // ============= 대댓글 관리 =============

    /**
     * 대댓글 추가
     * 양방향 연관관계 편의 메서드
     */
    public void addReply(NoteComment reply) {
        if (reply == null) {
            throw new IllegalArgumentException("대댓글 정보는 필수입니다");
        }

        if (!this.isTopLevel()) {
            throw new IllegalStateException("대댓글에는 답글을 달 수 없습니다");
        }

        this.replies.add(reply);
        if (reply.getParentComment() != this) {
            reply.setParentComment(this);
        }
    }

    /**
     * 대댓글 제거
     */
    public void removeReply(NoteComment reply) {

        if (reply == null) {
            return;
        }
        this.replies.remove(reply);
    }

    /**
     * 대댓글 개수 조회
     */
    public int getReplyCount() {
        return this.replies.size();
    }

    // ============= Soft Delete =============

    /**
     * Soft Delete
     * 대댓글도 함께 삭제됨 (cascade)
     */
    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();

        if (this.replies != null) {
            this.replies.forEach(NoteComment::delete);
        }
    }

    /**
     * 복구
     */
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }
}
