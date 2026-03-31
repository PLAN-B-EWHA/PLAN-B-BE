package myexpressionfriend_api.scenario.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 사회적 상호작용 시나리오
 *
 * <p>scenario_id 는 "W01_MJ_001" 형식의 비즈니스 키를 PK로 사용합니다.</p>
 */
@Entity
@Table(name = "scenarios")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Scenario {

    // ── PK ────────────────────────────────────────────────────────────
    @Id
    @Column(name = "scenario_id", length = 50, updatable = false, nullable = false)
    private String scenarioId;                  // e.g. "W01_MJ_001"

    // ── metadata ──────────────────────────────────────────────────────
    @Column(nullable = false)
    private Integer week;

    @Column(length = 100)
    private String theme;

    @Column(name = "relationship_stage", length = 50)
    private String relationshipStage;

    @Column(name = "scenario_seed", columnDefinition = "TEXT")
    private String scenarioSeed;

    @Column(name = "lobby_title", length = 100)
    private String lobbyTitle;

    @Column(name = "background_image_id", length = 100)
    private String backgroundImageId;

    // ── cast ──────────────────────────────────────────────────────────
    @Column(name = "main_character", length = 50)
    private String mainCharacter;

    @Column(name = "main_char_pos", length = 20)
    private String mainCharPos;

    @Column(name = "sub_characters", length = 100)
    private String subCharacters;               // nullable

    @Column(name = "sub_char_pos", length = 20)
    private String subCharPos;                  // nullable

    // ── final_summary ─────────────────────────────────────────────────
    @Column(name = "final_learning_point", columnDefinition = "TEXT")
    private String finalLearningPoint;

    // ── auditing ──────────────────────────────────────────────────────
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── relations ─────────────────────────────────────────────────────
    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("turnOrder ASC")
    @BatchSize(size = 100)
    @Builder.Default
    private List<ScenarioDialogueTurn> dialogueFlow = new ArrayList<>();
}
