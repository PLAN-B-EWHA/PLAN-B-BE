package com.planB.myexpressionfriend.common.domain.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "generated_reports", indexes = {
        @Index(name = "idx_generated_report_user", columnList = "user_id"),
        @Index(name = "idx_generated_report_status", columnList = "status"),
        @Index(name = "idx_generated_report_issued", columnList = "issued_at"),
        @Index(name = "idx_generated_report_created", columnList = "created_at")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class GeneratedReport {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "report_id", updatable = false, nullable = false)
    private UUID reportId;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "target_child_id")
    private UUID targetChildId;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "preference_id")
    private UUID preferenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "report_body", columnDefinition = "TEXT")
    private String reportBody;

    @Column(name = "prompt_used", columnDefinition = "TEXT")
    private String promptUsed;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "period_start_at")
    private LocalDateTime periodStartAt;

    @Column(name = "period_end_at")
    private LocalDateTime periodEndAt;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void markGenerated(
            String title,
            String summary,
            String reportBody,
            String promptUsed,
            String modelName,
            LocalDateTime issuedAt
    ) {
        if (reportBody == null || reportBody.isBlank()) {
            throw new IllegalArgumentException("reportBody is required");
        }

        this.status = ReportStatus.GENERATED;
        this.title = title;
        this.summary = summary;
        this.reportBody = reportBody;
        this.promptUsed = promptUsed;
        this.modelName = modelName;
        this.issuedAt = issuedAt != null ? issuedAt : LocalDateTime.now();
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("failure reason is required");
        }
        this.status = ReportStatus.FAILED;
        this.failureReason = reason.length() > 1000 ? reason.substring(0, 1000) : reason;
    }

    public void markSkipped(String reason) {
        this.status = ReportStatus.SKIPPED;
        this.failureReason = (reason == null || reason.isBlank()) ? "Skipped by policy" : reason;
    }
}
