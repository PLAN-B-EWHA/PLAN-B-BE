package myexpressionfriend_api.scenario.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

/**
 * 대화 턴의 선택지 (options 배열의 각 요소)
 *
 * <p>score(0·1·2)는 서버에서 판정 로직에 사용하므로 int로 유지합니다.
 * animation/expression 등 Unity 렌더링 속성은 String으로 저장합니다.</p>
 */
@Entity
@Table(name = "dialogue_options", indexes = {
        @Index(name = "idx_do_turn_order", columnList = "turn_id, option_order")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DialogueOption {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turn_id", nullable = false)
    private ScenarioDialogueTurn turn;

    /** 화면 렌더링 순서 (0-based) */
    @Column(name = "option_order", nullable = false)
    private Integer optionOrder;

    /** 선택 점수: 0(Bad) · 1(Acceptable) · 2(Good/Best) */
    @Column(nullable = false)
    private Integer score;

    /** 선택지 텍스트 */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    /** PEERS 행동 근거 설명 */
    @Column(name = "peers_logic", columnDefinition = "TEXT")
    private String peersLogic;

    /** 피드백 메시지 (score=2 Best 일 때 null) */
    @Column(columnDefinition = "TEXT")
    private String feedback;

    /** NPC 반응 라인 목록 ["민준: ...", "민준: ..."] */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "npc_reaction", columnDefinition = "jsonb")
    private List<String> npcReaction;

    /** Unity 반응 애니메이션 클립 이름 */
    @Column(name = "reaction_animation", length = 60)
    private String reactionAnimation;

    /** Unity 반응 표정 에셋 이름 */
    @Column(name = "reaction_expression", length = 30)
    private String reactionExpression;
}
