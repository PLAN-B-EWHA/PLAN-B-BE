package myexpressionfriend_api.player.domain;

import jakarta.persistence.*;
import lombok.*;
import myexpressionfriend_api.auth.domain.user.User;
import myexpressionfriend_api.child.domain.Child;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "game_player_selections",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_game_player_selections_user", columnNames = "user_id")
        },
        indexes = {
                @Index(name = "idx_game_player_selections_child", columnList = "child_id")
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class GamePlayerSelection {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "selection_id", updatable = false, nullable = false)
    private UUID selectionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @CreatedDate
    @Column(name = "selected_at", nullable = false, updatable = false)
    private LocalDateTime selectedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void changeChild(Child child) {
        if (child == null) {
            throw new IllegalArgumentException("child is required");
        }
        this.child = child;
    }
}