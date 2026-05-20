package myexpressionfriend_api.game.domain;

import jakarta.persistence.*;
import lombok.*;
import myexpressionfriend_api.child.domain.Child;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dialogue_turns", indexes = {
        @Index(name = "idx_dialogue_turns_session", columnList = "session_id"),
        @Index(name = "idx_dialogue_turns_child", columnList = "child_id")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DialogueTurn {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "turn_id", updatable = false, nullable = false)
    private UUID turnId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private DialogueSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Column(name = "turn_number", nullable = false)
    private Integer turnNumber;

    @Column(name = "selected_option_order", nullable = false)
    private Integer selectedOptionOrder;

    @Column(name = "selected_score", nullable = false)
    private Integer selectedScore;

    @Column(name = "npc_reaction_expression", length = 30)
    private String npcReactionExpression;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
