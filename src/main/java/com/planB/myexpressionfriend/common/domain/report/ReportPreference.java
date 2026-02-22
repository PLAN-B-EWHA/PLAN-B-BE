package com.planB.myexpressionfriend.common.domain.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "report_preferences", indexes = {
        @Index(name = "idx_report_pref_user", columnList = "user_id", unique = true),
        @Index(name = "idx_report_pref_enabled_next", columnList = "is_enabled,next_issue_at")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ReportPreference {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "preference_id", updatable = false, nullable = false)
    private UUID preferenceId;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 20)
    @Builder.Default
    private ReportScheduleType scheduleType = ReportScheduleType.WEEKLY;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_channel", nullable = false, length = 20)
    @Builder.Default
    private ReportDeliveryChannel deliveryChannel = ReportDeliveryChannel.IN_APP;

    @Column(name = "delivery_time", nullable = false)
    @Builder.Default
    private LocalTime deliveryTime = LocalTime.of(9, 0);

    @Column(name = "timezone", nullable = false, length = 50)
    @Builder.Default
    private String timezone = "Asia/Seoul";

    @Enumerated(EnumType.STRING)
    @Column(name = "child_scope", nullable = false, length = 30)
    @Builder.Default
    private ReportChildScope childScope = ReportChildScope.ALL_CHILDREN;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "target_child_id")
    private UUID targetChildId;

    @Column(name = "language", nullable = false, length = 10)
    @Builder.Default
    private String language = "ko";

    @Column(name = "model_name", nullable = false, length = 100)
    @Builder.Default
    private String modelName = "default";

    @Column(name = "prompt_template", columnDefinition = "TEXT")
    private String promptTemplate;

    @Column(name = "max_tokens", nullable = false)
    @Builder.Default
    private Integer maxTokens = 1200;

    @Column(name = "auto_issue_on_no_data", nullable = false)
    @Builder.Default
    private Boolean autoIssueOnNoData = false;

    @Column(name = "cooldown_hours", nullable = false)
    @Builder.Default
    private Integer cooldownHours = 24;

    @Column(name = "last_issued_at")
    private LocalDateTime lastIssuedAt;

    @Column(name = "next_issue_at")
    private LocalDateTime nextIssueAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        validateChildScope();
        validateTokenAndCooldown();
    }

    public void updateDelivery(ReportScheduleType scheduleType, LocalTime deliveryTime, String timezone) {
        if (scheduleType == null) {
            throw new IllegalArgumentException("scheduleType is required");
        }
        if (deliveryTime == null) {
            throw new IllegalArgumentException("deliveryTime is required");
        }
        if (timezone == null || timezone.isBlank()) {
            throw new IllegalArgumentException("timezone is required");
        }

        this.scheduleType = scheduleType;
        this.deliveryTime = deliveryTime;
        this.timezone = timezone.trim();
    }

    public void updateChildScope(ReportChildScope childScope, UUID targetChildId) {
        if (childScope == null) {
            throw new IllegalArgumentException("childScope is required");
        }
        this.childScope = childScope;
        this.targetChildId = targetChildId;
        validateChildScope();
    }

    public void updatePrompt(String promptTemplate, String modelName, Integer maxTokens) {
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("modelName is required");
        }
        this.promptTemplate = (promptTemplate == null || promptTemplate.isBlank()) ? null : promptTemplate.trim();
        this.modelName = modelName.trim();

        if (maxTokens == null || maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
        this.maxTokens = maxTokens;
    }

    public void changeDeliveryChannel(ReportDeliveryChannel deliveryChannel) {
        if (deliveryChannel == null) {
            throw new IllegalArgumentException("deliveryChannel is required");
        }
        this.deliveryChannel = deliveryChannel;
    }

    public void changeLanguage(String language) {
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("language is required");
        }
        this.language = language.trim();
    }

    public void updateIssuePolicy(Boolean autoIssueOnNoData, Integer cooldownHours) {
        this.autoIssueOnNoData = autoIssueOnNoData != null && autoIssueOnNoData;
        this.cooldownHours = cooldownHours;
        validateTokenAndCooldown();
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public void markIssued(LocalDateTime issuedAt, LocalDateTime nextIssueAt) {
        this.lastIssuedAt = issuedAt;
        this.nextIssueAt = nextIssueAt;
    }

    public void updateNextIssueAt(LocalDateTime nextIssueAt) {
        this.nextIssueAt = nextIssueAt;
    }

    private void validateChildScope() {
        if (this.childScope == ReportChildScope.SPECIFIC_CHILD && this.targetChildId == null) {
            throw new IllegalStateException("targetChildId is required when childScope is SPECIFIC_CHILD");
        }

        if (this.childScope == ReportChildScope.ALL_CHILDREN) {
            this.targetChildId = null;
        }
    }

    private void validateTokenAndCooldown() {
        if (this.maxTokens != null && this.maxTokens <= 0) {
            throw new IllegalStateException("maxTokens must be positive");
        }
        if (this.cooldownHours == null || this.cooldownHours < 0) {
            throw new IllegalStateException("cooldownHours must be zero or positive");
        }
    }
}
