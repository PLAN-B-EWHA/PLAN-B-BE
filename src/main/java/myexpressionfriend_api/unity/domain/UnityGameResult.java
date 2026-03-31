package myexpressionfriend_api.unity.domain;

import jakarta.persistence.*;
import lombok.*;
import myexpressionfriend_api.game.domain.GameSession;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Unity 게임 플레이 결과 저장 엔티티
 */
@Entity
@Table(name = "unity_game_results", indexes = {
        @Index(name = "idx_unity_game_results_session_id", columnList = "game_session_id"),
        @Index(name = "idx_unity_game_results_mission_id", columnList = "mission_id"),
        @Index(name = "idx_unity_game_results_unity_mission_id", columnList = "unity_mission_id"),
        @Index(name = "idx_unity_game_results_created_at", columnList = "created_at")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UnityGameResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unity_game_result_id", updatable = false, nullable = false)
    private Long unityGameResultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id")
    private GameSession gameSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unity_mission_id")
    private UnityMission unityMission;

    @Column(name = "mission_id", nullable = false)
    private Integer missionId;

    @Column(name = "is_success", nullable = false)
    private Boolean success;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "duration_seconds", nullable = false)
    private Float durationSeconds;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
