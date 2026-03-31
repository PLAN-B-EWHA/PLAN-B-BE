package myexpressionfriend_api.therapist.memo.domain;

import jakarta.persistence.*;
import lombok.*;
import myexpressionfriend_api.child.domain.Child;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "therapist_memo", indexes = {
        @Index(name = "idx_therapist_memo_child_id", columnList = "child_id"),
        @Index(name = "idx_therapist_memo_therapist_id", columnList = "therapist_id"),
        @Index(name = "idx_therapist_memo_week_of", columnList = "week_of"),
        @Index(name = "idx_therapist_memo_status", columnList = "status")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class TherapistMemo {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Column(name = "therapist_id", nullable = false, columnDefinition = "uuid")
    private UUID therapistId;

    @Column(name = "week_of", nullable = false)
    private LocalDate weekOf;

    /** 치료사 내부 세션 노트 (보호자 비공개) */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** LLM이 생성한 보호자용 내용 */
    @Column(name = "parent_content", columnDefinition = "TEXT")
    private String parentContent;

    /** 가정 연습 팁 */
    @Column(name = "home_practice_tip", columnDefinition = "TEXT")
    private String homePracticeTip;

    @Column(name = "is_visible_to_parent", nullable = false)
    @Builder.Default
    private Boolean isVisibleToParent = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TherapistMemoSource source = TherapistMemoSource.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TherapistMemoStatus status = TherapistMemoStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ─── 도메인 메서드 ──────────────────────────────────────────────────

    public void updateDraft(String content, String parentContent, String homePracticeTip, Boolean isVisibleToParent) {
        if (content != null) this.content = content;
        if (parentContent != null) this.parentContent = parentContent;
        if (homePracticeTip != null) this.homePracticeTip = homePracticeTip;
        if (isVisibleToParent != null) this.isVisibleToParent = isVisibleToParent;
        this.source = TherapistMemoSource.MANUAL;
    }

    /**
     * LLM 초안 적용 - 이미 입력된 내용은 덮어쓰지 않음.
     * 치료사가 직접 입력한 값이 있으면 그 필드는 유지됩니다.
     */
    public void applyLlmDraft(String parentContent, String homePracticeTip) {
        if (this.parentContent == null || this.parentContent.isBlank()) {
            this.parentContent = parentContent;
        }
        if (this.homePracticeTip == null || this.homePracticeTip.isBlank()) {
            this.homePracticeTip = homePracticeTip;
        }
        this.source = TherapistMemoSource.LLM_DRAFT;
    }

    /**
     * LLM 초안 강제 적용 - 치료사가 명시적으로 재생성 요청할 때만 사용.
     */
    public void forceApplyLlmDraft(String parentContent, String homePracticeTip) {
        this.parentContent = parentContent;
        this.homePracticeTip = homePracticeTip;
        this.source = TherapistMemoSource.LLM_DRAFT;
    }

    public void publish() {
        this.status = TherapistMemoStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public boolean isDraft() {
        return TherapistMemoStatus.DRAFT.equals(this.status);
    }

    public boolean isOwnedBy(UUID therapistId) {
        return this.therapistId.equals(therapistId);
    }
}
