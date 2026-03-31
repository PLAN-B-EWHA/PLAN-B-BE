package myexpressionfriend_api.homework.domain;

import jakarta.persistence.*;
import lombok.*;
import myexpressionfriend_api.auth.domain.user.User;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "homework_reports", indexes = {
        @Index(name = "idx_hr_homework", columnList = "homework_id")
})
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

    // ── 연관 ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_id", nullable = false)
    private HomeworkAssignment homework;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by", nullable = false)
    private User reportedBy;

    // ── 수행 결과 ─────────────────────────────────────────────────────

    /** 숙제 완료 여부 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private CompletionStatus completed;

    /** 시작 유형: 자발적 / 힌트 / 유도 */
    @Enumerated(EnumType.STRING)
    @Column(name = "initiated_by", length = 20)
    private InitiationType initiatedBy;

    /**
     * 실제로 적용한 전략.
     * 계획(HomeworkAssignment.strategyFocus)과 다를 수 있어 별도 기록합니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_applied", length = 40)
    private StrategyFocus strategyApplied;

    // ── 관찰 내용 ─────────────────────────────────────────────────────

    /** 보호자 자유 관찰 텍스트 */
    @Column(name = "parent_observation", columnDefinition = "TEXT")
    private String parentObservation;

    /** 상대방(또래) 반응 관찰 ("친구가 웃으면서 더 이야기했어요") */
    @Column(name = "peer_response_observed", columnDefinition = "TEXT")
    private String peerResponseObserved;

    /**
     * 자발적 시도 여부.
     * initiated_by=SELF 와 구별: 숙제와 무관하게 일상에서 자연스럽게 시도한 경우 true.
     */
    @Column(name = "spontaneous_flag", nullable = false)
    @Builder.Default
    private Boolean spontaneousFlag = false;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    @PrePersist
    protected void prePersist() {
        if (reportedAt == null) reportedAt = LocalDateTime.now();
    }
}
