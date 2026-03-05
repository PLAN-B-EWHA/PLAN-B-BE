package com.planB.myexpressionfriend.common.domain.note;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
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
 *
 *
 */
@Entity
@Table(name = "children_notes", indexes = {
        @Index(name = "idx_notes_child", columnList = "child_id"),
        @Index(name = "idx_notes_author", columnList = "author_id"),
        @Index(name = "idx_notes_type", columnList = "note_type"),
        @Index(name = "idx_notes_created", columnList = "created_at"),
        @Index(name = "idx_notes_deleted", columnList = "is_deleted")
})
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE children_notes SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE note_id = ?")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"child", "author", "assets", "comments"})
@EntityListeners(AuditingEntityListener.class)
public class ChildNote {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "note_id", updatable = false, nullable = false)
    private UUID noteId;

    /**
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    /**
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, length = 20)
    private NoteType type;

    /**
     */
    @Column(length = 200)
    private String title;

    /**
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     */
    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NoteAsset> assets = new ArrayList<>();

    /**
     */
    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NoteComment> comments = new ArrayList<>();

    /**
     * Soft Delete
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    /**
     */
    public void changeTitle(String title) {
        if (title != null && title.length() > 200) {
            throw new IllegalArgumentException("제목은 200자를 초과할 수 없습니다.");
        }
        this.title = title;
    }

    /**
     */
    public void changeContent(String content) {

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content is required.");
        }
        if (content.length() > 50000) {
            throw new IllegalArgumentException("본문은 50,000자를 초과할 수 없습니다.");
        }
        this.content = content.trim();
    }

    /**
     */
    public boolean isAuthor(UUID userId) {
        return this.author.getUserId().equals(userId);
    }

    /**
     */
    public boolean canEdit(UUID userId) {
        return isAuthor(userId);
    }

    /**
     */
    public boolean canDelete(UUID userId) {
        return isAuthor(userId) || child.isPrimaryParent(userId);
    }


    /**
     */
    public void addAsset(NoteAsset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset is required.");
        }

        if (this.assets.size() >= 5) {
            throw new IllegalStateException("Up to 5 attachments are allowed per note.");
        }

        this.assets.add(asset);
        if (asset.getNote() != this) {
            asset.setNote(this);
        }
    }

    /**
     */
    public void removeAsset(NoteAsset asset) {
        if (asset == null) {
            return;
        }
        this.assets.remove(asset);
    }

    /**
     */
    public void clearAssets() {
        this.assets.clear();
    }


    /**
     */
    public void addComment(NoteComment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("Comment is required.");
        }
        this.comments.add(comment);
        if (comment.getNote() != this) {
            comment.setNote(this);
        }
    }

    /**
     */
    public void removeComment(NoteComment comment) {

        if (comment == null) {
            return;
        }
        this.comments.remove(comment);
    }

    // ============= Soft Delete =============

    /**
     * Soft Delete
     */
    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     */
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }
}
