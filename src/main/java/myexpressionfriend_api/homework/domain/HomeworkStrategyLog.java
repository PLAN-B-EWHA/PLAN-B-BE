package myexpressionfriend_api.homework.domain;

import jakarta.persistence.*;
import lombok.*;
import myexpressionfriend_api.child.domain.Child;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 전략별 시도 이력 및 효과 추적.
 *
 * <p>HomeworkReport 제출 시 또는 치료사 검토 시 생성합니다.
 * 아동별·전략별 성공률, 시도 횟수 추이를 집계할 수 있습니다.</p>
 */
@Entity
@Table(name = "homework_strategy_logs", indexes = {
        @Index(name = "idx_hsl_child_strategy", columnList = "child_id, strategy"),
        @Index(name = "idx_hsl_homework",       columnList = "homework_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HomeworkStrategyLog {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID logId;

    // ── 연관 ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_id", nullable = false)
    private HomeworkAssignment homework;

    // ── 전략 시도 내용 ────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(length = 40, nullable = false)
    private StrategyFocus strategy;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private StrategyOutcome outcome;

    /** 해당 전략을 이 아동이 몇 번째 시도하는지 (누적 카운트) */
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "therapist_note", columnDefinition = "TEXT")
    private String therapistNote;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
