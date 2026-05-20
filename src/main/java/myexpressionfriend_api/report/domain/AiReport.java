package myexpressionfriend_api.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import myexpressionfriend_api.auth.domain.user.User;
import myexpressionfriend_api.child.domain.Child;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_reports", indexes = {
        @Index(name = "idx_ai_report_child_created", columnList = "child_id, created_at"),
        @Index(name = "idx_ai_report_child_status", columnList = "child_id, status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiReport {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "report_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", length = 20, nullable = false)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.DRAFT;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "rag_context_snapshot", columnDefinition = "TEXT")
    private String ragContextSnapshot;

    @Column(name = "prompt_snapshot", columnDefinition = "TEXT")
    private String promptSnapshot;

    @Column(length = 100)
    private String model;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public void updateDraft(String title, String content) {
        if (this.status == ReportStatus.PUBLISHED || this.status == ReportStatus.ARCHIVED) {
            throw new IllegalStateException("Published or archived reports cannot be edited.");
        }
        this.title = title;
        this.content = content;
    }

    public void review(User reviewer, String title, String content) {
        if (this.status == ReportStatus.PUBLISHED || this.status == ReportStatus.ARCHIVED) {
            throw new IllegalStateException("Published or archived reports cannot be reviewed.");
        }
        this.title = title;
        this.content = content;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.status = ReportStatus.REVIEWED;
    }

    public void publish(User reviewer) {
        if (this.status != ReportStatus.REVIEWED) {
            throw new IllegalStateException("Only reviewed reports can be published. status=" + this.status);
        }
        this.reviewedBy = reviewer;
        if (this.reviewedAt == null) {
            this.reviewedAt = LocalDateTime.now();
        }
        this.publishedAt = LocalDateTime.now();
        this.status = ReportStatus.PUBLISHED;
    }

    public void archive() {
        this.status = ReportStatus.ARCHIVED;
    }
}
