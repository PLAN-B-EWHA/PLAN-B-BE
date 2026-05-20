package myexpressionfriend_api.game.domain;

import jakarta.persistence.*;
import lombok.*;
import myexpressionfriend_api.child.domain.Child;
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
@Table(name = "expression_sessions", indexes = {
        @Index(name = "idx_expression_sessions_child", columnList = "child_id"),
        @Index(name = "idx_expression_sessions_emotion", columnList = "emotion_target")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ExpressionSession {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "session_id", updatable = false, nullable = false)
    private UUID sessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Column(name = "emotion_target", nullable = false, length = 50)
    private String emotionTarget;

    @Column(name = "final_accuracy", nullable = false)
    private Float finalAccuracy;

    @Column(name = "is_success", nullable = false)
    private Boolean isSuccess;

    @Column(name = "total_tries", nullable = false)
    private Integer totalTries;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at", nullable = false)
    private Instant endedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpressionTry> tries = new ArrayList<>();
}
