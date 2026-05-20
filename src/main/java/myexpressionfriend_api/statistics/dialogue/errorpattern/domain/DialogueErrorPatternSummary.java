package myexpressionfriend_api.statistics.dialogue.errorpattern.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@Table(name = "dialogue_error_pattern_summary")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DialogueErrorPatternSummary {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Convert(converter = PeersThemeConverter.class)
    @Column(name = "theme", nullable = false, length = 100)
    private PeersTheme theme;

    @Column(name = "analyzed_turn_count", nullable = false)
    @Builder.Default
    private Integer analyzedTurnCount = 0;

    @Column(name = "lecturing_rate", nullable = false)
    @Builder.Default
    private Double lecturingRate = 0.0;

    @Column(name = "criticism_rate", nullable = false)
    @Builder.Default
    private Double criticismRate = 0.0;

    @Column(name = "topic_ignore_rate", nullable = false)
    @Builder.Default
    private Double topicIgnoreRate = 0.0;

    @Column(name = "rejection_rate", nullable = false)
    @Builder.Default
    private Double rejectionRate = 0.0;

    @Column(name = "unclassified_rate", nullable = false)
    @Builder.Default
    private Double unclassifiedRate = 0.0;

    @Column(name = "reliability_rate", nullable = false)
    @Builder.Default
    private Double reliabilityRate = 0.0;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "last_refreshed_at")
    private LocalDateTime lastRefreshedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updateRates(
            int analyzedTurnCount,
            double lecturingRate,
            double criticismRate,
            double topicIgnoreRate,
            double rejectionRate,
            double unclassifiedRate,
            double reliabilityRate,
            String modelName,
            LocalDateTime refreshedAt
    ) {
        this.analyzedTurnCount = analyzedTurnCount;
        this.lecturingRate = lecturingRate;
        this.criticismRate = criticismRate;
        this.topicIgnoreRate = topicIgnoreRate;
        this.rejectionRate = rejectionRate;
        this.unclassifiedRate = unclassifiedRate;
        this.reliabilityRate = reliabilityRate;
        this.modelName = modelName;
        this.lastRefreshedAt = refreshedAt;
    }
}
