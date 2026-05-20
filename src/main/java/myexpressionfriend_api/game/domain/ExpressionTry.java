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
@Table(name = "expression_tries", indexes = {
        @Index(name = "idx_expression_tries_session", columnList = "session_id"),
        @Index(name = "idx_expression_tries_child", columnList = "child_id")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ExpressionTry {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "try_id", updatable = false, nullable = false)
    private UUID tryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ExpressionSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @Column(name = "try_number", nullable = false)
    private Integer tryNumber;

    @Column(name = "accuracy_score", nullable = false)
    private Float accuracyScore;

    @Column(name = "duration_ms", nullable = false)
    private Integer durationMs;

    @Column(name = "is_success", nullable = false)
    private Boolean isSuccess;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
