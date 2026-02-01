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
 * 아동 치료 노트 엔티티
 *
 * 기능:
 * - 치료사 소견, 부모 관찰 일지, 시스템 자동 기록
 * - 이미지/비디모/문서 첨부 가능
 * - 댓글을 통한 소통 지원
 *
 * 권한:
 * - 작성: WRITE_NOTE 권한 필요
 * - 조회: VIEW_REPORT 권한 필요
 * - 수정: 작성자 본인만
 * - 삭제: 작성자 본인 또는 주보호자
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
     * 대상 아동
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    /**
     * 작성자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * 노트 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, length = 20)
    private NoteType type;

    /**
     * 제목 (선택)
     */
    @Column(length = 200)
    private String title;

    /**
     * 본문 (마크다운 지원)
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 첨부 파일 목록
     */
    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NoteAsset> assets = new ArrayList<>();

    /**
     * 댓글 목록
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

    // ============= 비즈니스 메서드 =============

    /**
     * 제목 변경
     */
    public void changeTitle(String title) {
        if (title != null && title.length() > 200) {
            throw new IllegalArgumentException("제목은 200자를 초과할 수 없습니다");
        }
        this.title = title;
    }

    /**
     * 본문 변경
     */
    public void changeContent(String content) {

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("본문은 필수입니다");
        }
        if (content.length() > 50000) {
            throw new IllegalArgumentException("본문은 50,000자를 초과할 수 없습니다");
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
     * 수정 권한 확인
     */
    public boolean canEdit(UUID userId) {
        return isAuthor(userId);
    }

    /**
     * 삭제 권한 확인
     */
    public boolean canDelete(UUID userId) {
        return isAuthor(userId) || child.isPrimaryParent(userId);
    }

    // ============= 첨부파일 관리 =============

    /**
     * 첨부파일 추가
     * 양방향 연관관계 편의 메서드
     */
    public void addAsset(NoteAsset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("첨부파일 정보는 필수입니다");
        }

        // 최대 첨부파일 개수 제한
        if (this.assets.size() >= 10) {
            throw new IllegalStateException("노트당 최대 10개까지 파일을 첨부할 수 있습니다");
        }

        this.assets.add(asset);
        if (asset.getNote() != this) {
            asset.setNote(this);
        }
    }

    /**
     * 첨부파일 제거
     */
    public void removeAsset(NoteAsset asset) {
        if (asset == null) {
            return;
        }
        this.assets.remove(asset);
    }

    /**
     * 모든 첨부파일 제거
     */
    public void clearAssets() {
        this.assets.clear();
    }

    // ============= 댓글 관리 =============

    /**
     * 댓글 추가
     * 양방향 연관관계 편의 메서드
     */
    public void addComment(NoteComment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("댓글 정보는 필수입니다");
        }
        this.comments.add(comment);
        if (comment.getNote() != this) {
            comment.setNote(this);
        }
    }

    /**
     * 댓글 제거
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
     * 관련 첨부파일, 댓글도 함께 삭제됨 (cascade)
     */
    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 복구
     */
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }
}
