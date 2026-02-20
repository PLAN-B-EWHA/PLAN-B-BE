package com.planB.myexpressionfriend.common.domain.mission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "mission_category", nullable = false, length = 50)
    private MissionCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private MissionDifficulty difficulty;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String instructions;

    @Column(name = "expected_duration")
    private Integer expectedDuration;

    @Column(name = "is_llm_generated", nullable = false)
    @Builder.Default
    private Boolean llmGenerated = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

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

    public void changeTitle(String title) {
        if (title != null && title.length() > 200) {
            throw new IllegalArgumentException("제목은 200자를 초과할 수 없습니다.");
        }
        this.title = title;
    }

    public void changeDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("설명은 필수입니다.");
        }
        if (description.length() > 50000) {
            throw new IllegalArgumentException("설명은 50,000자를 초과할 수 없습니다.");
        }
        this.description = description.trim();
    }

    public void changeInstructions(String instructions) {
        if (instructions == null || instructions.isBlank()) {
            throw new IllegalArgumentException("수행 방법은 필수입니다.");
        }
        if (instructions.length() > 50000) {
            throw new IllegalArgumentException("수행 방법은 50,000자를 초과할 수 없습니다.");
        }
        this.instructions = instructions.trim();
    }

    public void changeCategory(MissionCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("카테고리는 필수입니다.");
        }
        this.category = category;
    }

    public void changeDifficulty(MissionDifficulty difficulty) {
        if (difficulty == null) {
            throw new IllegalArgumentException("난이도는 필수입니다.");
        }
        this.difficulty = difficulty;
    }

    public void changeExpectedDuration(Integer expectedDuration) {
        if (expectedDuration != null && expectedDuration <= 0) {
            throw new IllegalArgumentException("예상 소요 시간은 양수여야 합니다.");
        }
        this.expectedDuration = expectedDuration;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }
}
