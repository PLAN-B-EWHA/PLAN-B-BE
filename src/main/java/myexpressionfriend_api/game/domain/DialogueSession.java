package myexpressionfriend_api.game.domain;

import jakarta.persistence.*;
import lombok.*;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.common.converter.PeersThemeConverter;
import myexpressionfriend_api.common.domain.PeersTheme;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dialogue_sessions", indexes = {
        @Index(name = "idx_dialogue_sessions_child", columnList = "child_id"),
        @Index(name = "idx_dialogue_sessions_theme", columnList = "theme")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DialogueSession {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "session_id", updatable = false, nullable = false)
    private UUID sessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Column(name = "scenario_id", nullable = false, length = 50)
    private String scenarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario_source", nullable = false, length = 30)
    @Builder.Default
    private ScenarioSource scenarioSource = ScenarioSource.UNITY_LOCAL;

    @Convert(converter = PeersThemeConverter.class)
    @Column(name = "theme", nullable = false, length = 100)
    private PeersTheme theme;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    @Column(name = "max_score", nullable = false)
    private Integer maxScore;

    @Column(name = "score_rate", nullable = false)
    private Float scoreRate;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at", nullable = false)
    private Instant endedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DialogueTurn> turns = new ArrayList<>();
}
