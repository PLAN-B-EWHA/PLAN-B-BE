package myexpressionfriend_api.homework.domain;

import jakarta.persistence.*;
import lombok.*;
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
        @Index(name = "idx_hw_child_week",   columnList = "child_id, week"),
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

    // ── 연관 ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    /**
     * 주차 진행 상황 FK (weekly_progress 테이블 — 구현 예정).
     * 현재는 UUID만 보관합니다.
     */
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "weekly_progress_id")
    private UUID weeklyProgressId;

    // ── 주차 & 전략 ───────────────────────────────────────────────────

    /** PEERS 커리큘럼 주차 (1~16) */
    @Column(nullable = false)
    private Integer week;

    /**
     * 해당 주차의 핵심 전략 주제.
     * week 와 1:1 대응이지만, 숙제별로 다른 전략을 부여할 수 있어 별도 보관합니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_focus", length = 40, nullable = false)
    private StrategyFocus strategyFocus;

    /** 전략 실행을 위한 기본 가이드 (치료사 작성) */
    @Column(columnDefinition = "TEXT")
    private String instruction;

    /** 보호자가 가정에서 실행할 Flash 문장 (LLM 또는 수동 생성) */
    @Column(name = "strategy_tip", columnDefinition = "TEXT")
    private String strategyTip;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_tip_source", length = 20)
    private StrategyTipSource strategyTipSource;

    // ── 일정 & 상태 ───────────────────────────────────────────────────

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private HomeworkStatus status = HomeworkStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── 도메인 메서드 ─────────────────────────────────────────────────

    public void updateStrategyTip(String tip, StrategyTipSource source) {
        this.strategyTip = tip;
        this.strategyTipSource = source;
    }

    public void submit() {
        if (this.status != HomeworkStatus.PENDING) {
            throw new IllegalStateException("제출 가능한 상태가 아닙니다. status=" + this.status);
        }
        this.status = HomeworkStatus.SUBMITTED;
    }

    public void review() {
        if (this.status != HomeworkStatus.SUBMITTED) {
            throw new IllegalStateException("검토 가능한 상태가 아닙니다. status=" + this.status);
        }
        this.status = HomeworkStatus.REVIEWED;
    }

    /** week 번호로 strategyFocus 자동 설정 */
    public static HomeworkAssignment createForWeek(Child child, int week) {
        return HomeworkAssignment.builder()
                .child(child)
                .week(week)
                .strategyFocus(StrategyFocus.ofWeek(week))
                .build();
    }
}
