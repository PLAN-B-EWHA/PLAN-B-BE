package com.planB.myexpressionfriend.common.domain.mission;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 홈 트레이닝 미션 템플릿 엔티티
 *
 * 기능:
 * - 치료사가 시스템 제공 템플릿 중 아동의 취약점에 맞는 과제를 선택하여 부여
 * - 기본 템플릿 + LLM 생성 템플릿 모두 저장
 *
 * 권한:
 * - 조회: THERAPIST, PARENT (할당받은 미션을 통해)
 * - 생성/수정: THERAPIST만 (시스템 관리)
 */
@Entity
@Table(name = "mission_templates", indexes = {
        @Index(name = "idx_templates_category", columnList = "mission_category"),
        @Index(name = "idx_templates_difficulty", columnList = "difficulty"),
        @Index(name = "idx_templates_active", columnList = "is_active"),
        @Index(name = "idx_templates_deleted", columnList = "is_deleted"),
        @Index(name = "idx_templates_created", columnList = "created_at")
})
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE mission_templates SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE template_id = ?")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@EntityListeners(AuditingEntityListener.class)
public class MissionTemplate {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "template_id", updatable = false, nullable = false)
    private UUID templateId;

    /**
     * 제목 (선택사항)
     */
    @Column(length = 200)
    private String title;

    /**
     * 설명
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    /**
     * 카테고리
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "mission_category", nullable = false, length = 50)
    private MissionCategory category;

    /**
     * 난이도
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private MissionDifficulty difficulty;

    /**
     * 수행 방법 (마크다운)
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String instructions;

    /**
     * 예상 소요 시간 (분) - 선택사항
     */
    @Column(name = "expected_duration")
    private Integer expectedDuration;

    /**
     * LLM 생성 여부
     */
    @Column(name = "is_llm_generated", nullable = false)
    @Builder.Default
    private Boolean llmGenerated = false;

    /**
     * 활성화 여부
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Soft Delete
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ============= 비즈니스 메서드 =============

    /**
     * 제목 변경
     */
    public void changeTitle(String title) {
        if (title != null && title.length() > 200) {
            throw new IllegalArgumentException("제목은 200자를 초과할 수 없습니다");
        }
        this.title = title;
    }

    /**
     * 설명 변경
     */
    public void changeDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("설명은 필수입니다");
        }
        if (description.length() > 50000) {
            throw new IllegalArgumentException("설명은 50,000자를 초과할 수 없습니다");
        }
        this.description = description.trim();
    }

    /**
     * 수행 방법 변경
     */
    public void changeInstructions(String instructions) {
        if (instructions == null || instructions.isBlank()) {
            throw new IllegalArgumentException("수행 방법은 필수입니다");
        }
        if (instructions.length() > 50000) {
            throw new IllegalArgumentException("수행 방법은 50,000자를 초과할 수 없습니다");
        }
        this.instructions = instructions.trim();
    }

    /**
     * 카테고리 변경
     */
    public void changeCategory(MissionCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("카테고리는 필수입니다");
        }
        this.category = category;
    }

    /**
     * 난이도 변경
     */
    public void changeDifficulty(MissionDifficulty difficulty) {
        if (difficulty == null) {
            throw new IllegalArgumentException("난이도는 필수입니다");
        }
        this.difficulty = difficulty;
    }

    /**
     * 예상 소요 시간 변경
     */
    public void changeExpectedDuration(Integer expectedDuration) {
        if (expectedDuration != null && expectedDuration <= 0) {
            throw new IllegalArgumentException("예상 소요 시간은 양수여야 합니다");
        }
        this.expectedDuration = expectedDuration;
    }

    /**
     * 활성화
     */
    public void activate() {
        this.active = true;
    }

    /**
     * 비활성화
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Soft Delete
     */
    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 복구
     */
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }

    /**
     * 활성화 여부 확인
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * LLM 생성 여부 확인
     */
    public boolean isLlmGenerated() {
        return this.llmGenerated;
    }
}