package myexpressionfriend_api.scenario.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 시나리오 내 대화 턴 (dialogue_flow 의 각 요소)
 */
@Entity
@Table(name = "scenario_dialogue_turns", indexes = {
        @Index(name = "idx_sdt_scenario_order", columnList = "scenario_id, turn_order")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScenarioDialogueTurn {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    /** JSON의 turn_id (1부터 시작하는 순서값) */
    @Column(name = "turn_order", nullable = false)
    private Integer turnOrder;

    @Column(name = "internal_monologue", columnDefinition = "TEXT")
    private String internalMonologue;

    /** NPC 발화 라인 목록 ["민준: ...", "민준: ..."] */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "npc_utterance", columnDefinition = "jsonb")
    private List<String> npcUtterance;

    /** Unity 애니메이션 클립 이름 (e.g. "Idle", "Happy_Hand_Gesture") */
    @Column(name = "npc_animation", length = 60)
    private String npcAnimation;

    /** Unity 표정 에셋 이름 (e.g. "Joy", "Neutral", "Anger") */
    @Column(name = "npc_expression", length = 30)
    private String npcExpression;

    // ── relations ─────────────────────────────────────────────────────
    @OneToMany(mappedBy = "turn", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("optionOrder ASC")
    @BatchSize(size = 100)
    @Builder.Default
    private List<DialogueOption> options = new ArrayList<>();
}
