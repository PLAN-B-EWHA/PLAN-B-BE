package myexpressionfriend_api.homework.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import myexpressionfriend_api.auth.domain.user.User;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "homework_reports",
        indexes = {@Index(name = "idx_hr_homework", columnList = "homework_id")},
        uniqueConstraints = {@UniqueConstraint(name = "uq_hr_homework_id", columnNames = "homework_id")}
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HomeworkReport {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_id", nullable = false)
    private HomeworkAssignment homework;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by", nullable = false)
    private User reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private CompletionStatus completed;

    @Enumerated(EnumType.STRING)
    @Column(name = "initiated_by", length = 20)
    private InitiationType initiatedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_applied", length = 40)
    private StrategyFocus strategyApplied;

    @Column(name = "parent_observation", columnDefinition = "TEXT")
    private String parentObservation;

    @Column(name = "peer_response_observed", columnDefinition = "TEXT")
    private String peerResponseObserved;

    @Column(name = "spontaneous_flag", nullable = false)
    @Builder.Default
    private Boolean spontaneousFlag = false;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    @Column(name = "therapist_review_comment", columnDefinition = "TEXT")
    private String therapistReviewComment;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void prePersist() {
        if (reportedAt == null) {
            reportedAt = LocalDateTime.now();
        }
    }

    public void review(User reviewer, String reviewComment) {
        this.reviewedBy = reviewer;
        this.therapistReviewComment = reviewComment;
        this.reviewedAt = LocalDateTime.now();
    }

    public void updateSubmission(
            CompletionStatus completed,
            InitiationType initiatedBy,
            StrategyFocus strategyApplied,
            String parentObservation,
            String peerResponseObserved,
            Boolean spontaneousFlag
    ) {
        this.completed = completed;
        this.initiatedBy = initiatedBy;
        this.strategyApplied = strategyApplied;
        this.parentObservation = parentObservation;
        this.peerResponseObserved = peerResponseObserved;
        this.spontaneousFlag = Boolean.TRUE.equals(spontaneousFlag);
        this.reportedAt = LocalDateTime.now();
    }
}
