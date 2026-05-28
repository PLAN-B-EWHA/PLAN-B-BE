package myexpressionfriend_api.scenario.domain;

import jakarta.persistence.*;
import lombok.*;
import myexpressionfriend_api.game.domain.ScenarioSource;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    @Builder.Default
    private ScenarioSource source = ScenarioSource.SERVER_MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 30)
    @Builder.Default
    private ScenarioApprovalStatus approvalStatus = ScenarioApprovalStatus.PUBLISHED;

    @Column(name = "generated_by_user_id")
    private UUID generatedByUserId;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "llm_prompt", columnDefinition = "TEXT")
    private String llmPrompt;

    @Column(name = "llm_model", length = 100)
    private String llmModel;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

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

    public void publish(UUID reviewerId, String reviewNote) {
        this.approvalStatus = ScenarioApprovalStatus.PUBLISHED;
        this.reviewedByUserId = reviewerId;
        this.reviewNote = reviewNote;
        this.publishedAt = LocalDateTime.now();
        this.archivedAt = null;
    }

    public void reject(UUID reviewerId, String reviewNote) {
        this.approvalStatus = ScenarioApprovalStatus.REJECTED;
        this.reviewedByUserId = reviewerId;
        this.reviewNote = reviewNote;
    }

    public void archive(UUID reviewerId, String reviewNote) {
        this.approvalStatus = ScenarioApprovalStatus.ARCHIVED;
        this.reviewedByUserId = reviewerId;
        this.reviewNote = reviewNote;
        this.archivedAt = LocalDateTime.now();
    }

    public void changeApprovalStatus(ScenarioApprovalStatus status, UUID reviewerId, String reviewNote) {
        this.approvalStatus = status;
        this.reviewedByUserId = reviewerId;
        this.reviewNote = reviewNote;

        if (status == ScenarioApprovalStatus.PUBLISHED) {
            this.publishedAt = LocalDateTime.now();
            this.archivedAt = null;
            return;
        }

        this.publishedAt = null;
        this.archivedAt = status == ScenarioApprovalStatus.ARCHIVED ? LocalDateTime.now() : null;
    }
}
