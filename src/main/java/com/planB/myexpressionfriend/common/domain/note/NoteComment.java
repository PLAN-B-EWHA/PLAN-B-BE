package com.planB.myexpressionfriend.common.domain.note;

import com.planB.myexpressionfriend.common.domain.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    @Setter
    private ChildNote note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    @Setter
    private NoteComment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NoteComment> replies = new ArrayList<>();

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

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

    public void changeContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("댓글 내용은 필수입니다.");
        }
        if (content.length() > 5000) {
            throw new IllegalArgumentException("댓글은 5,000자를 초과할 수 없습니다.");
        }
        this.content = content.trim();
    }

    public boolean isAuthor(UUID userId) {
        return this.author.getUserId().equals(userId);
    }

    public boolean isTopLevel() {
        return this.parentComment == null;
    }

    public boolean isReply() {
        return this.parentComment != null;
    }

    public boolean canEdit(UUID userId) {
        return isAuthor(userId);
    }

    public boolean canDelete(UUID userId) {
        return isAuthor(userId) || note.getChild().isPrimaryParent(userId);
    }

    public void addReply(NoteComment reply) {
        if (reply == null) {
            throw new IllegalArgumentException("대댓글 정보가 필요합니다.");
        }
        if (!this.isTopLevel()) {
            throw new IllegalStateException("대댓글은 최상위 댓글에만 추가할 수 있습니다.");
        }
        this.replies.add(reply);
        if (reply.getParentComment() != this) {
            reply.setParentComment(this);
        }
    }

    public void removeReply(NoteComment reply) {
        if (reply == null) {
            return;
        }
        this.replies.remove(reply);
    }

    public int getReplyCount() {
        return this.replies.size();
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        if (this.replies != null) {
            this.replies.forEach(NoteComment::delete);
        }
    }

    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }
}
