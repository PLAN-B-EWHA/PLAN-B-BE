package myexpressionfriend_api.homework.domain;

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
import myexpressionfriend_api.child.domain.Child;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "homework_assignments", indexes = {
        @Index(name = "idx_hw_child_week", columnList = "child_id, week"),
        @Index(name = "idx_hw_child_status", columnList = "child_id, status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HomeworkAssignment {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID homeworkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "weekly_progress_id")
    private UUID weeklyProgressId;

    @Column(nullable = false)
    private Integer week;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_focus", length = 40, nullable = false)
    private StrategyFocus strategyFocus;

    @Column(columnDefinition = "TEXT")
    private String instruction;

    @Column(name = "strategy_tip", columnDefinition = "TEXT")
    private String strategyTip;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_tip_source", length = 20)
    private StrategyTipSource strategyTipSource;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private HomeworkStatus status = HomeworkStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public void updateStrategyTip(String tip, StrategyTipSource source) {
        this.strategyTip = tip;
        this.strategyTipSource = source;
    }

    public void updateAssignment(
            Integer week,
            StrategyFocus strategyFocus,
            String instruction,
            String strategyTip,
            StrategyTipSource strategyTipSource,
            LocalDate dueDate
    ) {
        if (this.status != HomeworkStatus.PENDING) {
            throw new IllegalStateException("Only pending homework can be updated. status=" + this.status);
        }
        this.week = week;
        this.strategyFocus = strategyFocus;
        this.instruction = instruction;
        this.strategyTip = strategyTip;
        this.strategyTipSource = strategyTipSource;
        this.dueDate = dueDate;
    }

    public void submit() {
        if (this.status != HomeworkStatus.PENDING) {
            throw new IllegalStateException("Only pending homework can be submitted. status=" + this.status);
        }
        this.status = HomeworkStatus.SUBMITTED;
    }

    public void review() {
        if (this.status != HomeworkStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted homework can be reviewed. status=" + this.status);
        }
        this.status = HomeworkStatus.REVIEWED;
    }

    public void cancel() {
        if (this.status == HomeworkStatus.REVIEWED) {
            throw new IllegalStateException("Reviewed homework cannot be canceled.");
        }
        this.status = HomeworkStatus.CANCELED;
    }

    /** 기한 초과 자동 만료. 스케줄러 전용. */
    public void expire() {
        if (this.status != HomeworkStatus.PENDING) {
            throw new IllegalStateException("Only pending homework can be expired. status=" + this.status);
        }
        this.status = HomeworkStatus.EXPIRED;
    }

    public static HomeworkAssignment createForWeek(Child child, int week) {
        return HomeworkAssignment.builder()
                .child(child)
                .week(week)
                .strategyFocus(StrategyFocus.ofWeek(week))
                .build();
    }
}
