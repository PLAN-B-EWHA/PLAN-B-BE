package myexpressionfriend_api.statistics.dialogue.domain;

import jakarta.persistence.*;
import lombok.*;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.common.converter.PeersThemeConverter;
import myexpressionfriend_api.common.domain.PeersTheme;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dialogue_stat_summary")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DialogueStatSummary {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Convert(converter = PeersThemeConverter.class)
    @Column(name = "theme", nullable = false, length = 100)
    private PeersTheme theme;

    @Column(name = "score_rate", nullable = false)
    @Builder.Default
    private Double scoreRate = 0.0;

    @Column(name = "rapport_index", nullable = false)
    @Builder.Default
    private Double rapportIndex = 0.0;

    // 중간버전에서는 사용하지 않지만 DB 호환을 위해 유지한다.
    @Column(name = "turn_fatigue")
    private Double turnFatigue;

    @Column(name = "score0_rate", nullable = false)
    @Builder.Default
    private Double score0Rate = 0.0;

    @Column(name = "score1_rate", nullable = false)
    @Builder.Default
    private Double score1Rate = 0.0;

    @Column(name = "score2_rate", nullable = false)
    @Builder.Default
    private Double score2Rate = 0.0;

    @Column(name = "session_count", nullable = false)
    @Builder.Default
    private Integer sessionCount = 0;

    @Column(name = "ema_value")
    private Double emaValue;

    @Column(name = "ema_alpha")
    private Double emaAlpha;

    @Column(name = "consistency_std")
    private Double consistencyStd;

    // 중간버전에서는 사용하지 않지만 DB 호환을 위해 유지한다.
    @Column(name = "option_bias_detected")
    private Boolean optionBiasDetected;

    // 중간버전에서는 사용하지 않지만 DB 호환을 위해 유지한다.
    @Column(name = "biased_option_order")
    private Integer biasedOptionOrder;

    @Column(name = "retry_reduction_rate")
    private Double retryReductionRate;

    @Column(name = "trend_slope")
    private Double trendSlope;

    @Column(name = "trend_direction", length = 20)
    private String trendDirection;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "confidence_level", length = 20)
    private String confidenceLevel;

    @Column(name = "offline_reviewed_count", nullable = false)
    @Builder.Default
    private Integer offlineReviewedCount = 0;

    @Column(name = "offline_spontaneous_count", nullable = false)
    @Builder.Default
    private Integer offlineSpontaneousCount = 0;

    @Column(name = "offline_spontaneous_rate")
    private Double offlineSpontaneousRate;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void update(double scoreRate, double rapportIndex, Double turnFatigue,
                       double score0Rate, double score1Rate, double score2Rate, int sessionCount,
                       Double emaValue, Double emaAlpha, Double consistencyStd,
                       Boolean optionBiasDetected, Integer biasedOptionOrder, Double retryReductionRate,
                       Double trendSlope, String trendDirection,
                       Double confidenceScore, String confidenceLevel) {
        this.scoreRate = scoreRate;
        this.rapportIndex = rapportIndex;
        this.turnFatigue = turnFatigue;
        this.score0Rate = score0Rate;
        this.score1Rate = score1Rate;
        this.score2Rate = score2Rate;
        this.sessionCount = sessionCount;
        this.emaValue = emaValue;
        this.emaAlpha = emaAlpha;
        this.consistencyStd = consistencyStd;
        this.optionBiasDetected = optionBiasDetected;
        this.biasedOptionOrder = biasedOptionOrder;
        this.retryReductionRate = retryReductionRate;
        this.trendSlope = trendSlope;
        this.trendDirection = trendDirection;
        this.confidenceScore = confidenceScore;
        this.confidenceLevel = confidenceLevel;
    }

    public void updateOfflineOutcome(int reviewedCount, int spontaneousCount, Double spontaneousRate) {
        this.offlineReviewedCount = reviewedCount;
        this.offlineSpontaneousCount = spontaneousCount;
        this.offlineSpontaneousRate = spontaneousRate;
    }
}
