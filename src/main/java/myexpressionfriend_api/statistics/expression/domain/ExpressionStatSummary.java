package myexpressionfriend_api.statistics.expression.domain;

import jakarta.persistence.*;
import lombok.*;
import myexpressionfriend_api.child.domain.Child;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "expression_stat_summary")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ExpressionStatSummary {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Column(name = "emotion_target", nullable = false, length = 50)
    private String emotionTarget;

    @Column(name = "success_rate", nullable = false)
    @Builder.Default
    private Double successRate = 0.0;

    @Column(name = "fluency_index", nullable = false)
    @Builder.Default
    private Double fluencyIndex = 0.0;

    @Column(name = "avg_retry", nullable = false)
    @Builder.Default
    private Double avgRetry = 0.0;

    @Column(name = "session_count", nullable = false)
    @Builder.Default
    private Integer sessionCount = 0;

    @Column(name = "duration_decrease_rate", nullable = false)
    @Builder.Default
    private Double durationDecreaseRate = 0.0;

    // 중간버전에서는 사용하지 않지만 DB 호환을 위해 유지한다.
    @Column(name = "ci_lower")
    private Double ciLower;

    // 중간버전에서는 사용하지 않지만 DB 호환을 위해 유지한다.
    @Column(name = "ci_upper")
    private Double ciUpper;

    // 중간버전에서는 사용하지 않지만 DB 호환을 위해 유지한다.
    @Column(name = "convergence_speed")
    private Double convergenceSpeed;

    @Column(name = "valid_session_rate")
    private Double validSessionRate;

    @Column(name = "avg_session_duration_sec")
    private Double avgSessionDurationSec;

    @Column(name = "retry_reduction_rate")
    private Double retryReductionRate;

    @Column(name = "retry_baseline_status", length = 20)
    private String retryBaselineStatus;

    @Column(name = "trend_slope")
    private Double trendSlope;

    @Column(name = "trend_direction", length = 20)
    private String trendDirection;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "confidence_level", length = 20)
    private String confidenceLevel;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void update(double successRate, double fluencyIndex, double avgRetry,
                       int sessionCount, double durationDecreaseRate,
                       Double ciLower, Double ciUpper,
                       Double convergenceSpeed,
                       Double validSessionRate, Double avgSessionDurationSec,
                       Double retryReductionRate, String retryBaselineStatus,
                       Double trendSlope, String trendDirection,
                       Double confidenceScore, String confidenceLevel) {
        this.successRate = successRate;
        this.fluencyIndex = fluencyIndex;
        this.avgRetry = avgRetry;
        this.sessionCount = sessionCount;
        this.durationDecreaseRate = durationDecreaseRate;
        this.ciLower = ciLower;
        this.ciUpper = ciUpper;
        this.convergenceSpeed = convergenceSpeed;
        this.validSessionRate = validSessionRate;
        this.avgSessionDurationSec = avgSessionDurationSec;
        this.retryReductionRate = retryReductionRate;
        this.retryBaselineStatus = retryBaselineStatus;
        this.trendSlope = trendSlope;
        this.trendDirection = trendDirection;
        this.confidenceScore = confidenceScore;
        this.confidenceLevel = confidenceLevel;
    }
}
