package com.planB.myexpressionfriend.unity.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.planB.myexpressionfriend.common.domain.child.Child;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "unity_missions", indexes = {
        @Index(name = "idx_unity_missions_external_id", columnList = "external_mission_id"),
        @Index(name = "idx_unity_missions_type", columnList = "mission_type"),
        @Index(name = "idx_unity_missions_child_id", columnList = "child_id"),
        @Index(name = "idx_unity_missions_approval_status", columnList = "approval_status"),
        @Index(name = "idx_unity_missions_mission_date", columnList = "mission_date"),
        @Index(name = "idx_unity_missions_created_at", columnList = "created_at")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "child")
@EntityListeners(AuditingEntityListener.class)
public class UnityMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unity_mission_id", updatable = false, nullable = false)
    private Long unityMissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id")
    private Child child;

    @Column(name = "external_mission_id", nullable = false)
    private Integer missionId;

    @Column(name = "mission_name", nullable = false, length = 200)
    private String missionName;

    @Column(name = "mission_type", nullable = false, length = 50)
    private String missionTypeString;

    @Column(name = "target_keyword", nullable = false, length = 100)
    private String targetKeyword;

    @Column(name = "target_emotion", nullable = false, length = 100)
    private String targetEmotionString;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expression_data", columnDefinition = "jsonb")
    private JsonNode expressionData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "situation_data", columnDefinition = "jsonb")
    private JsonNode situationData;

    @Column(name = "mission_date")
    private LocalDate missionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private UnityMissionApprovalStatus approvalStatus = UnityMissionApprovalStatus.PENDING;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void approve(UUID approvedByUserId) {
        this.approvalStatus = UnityMissionApprovalStatus.APPROVED;
        this.approvedBy = approvedByUserId;
        this.approvedAt = LocalDateTime.now();
        this.rejectedReason = null;
    }

    public void reject(UUID rejectedByUserId, String reason) {
        this.approvalStatus = UnityMissionApprovalStatus.REJECTED;
        this.approvedBy = rejectedByUserId;
        this.approvedAt = LocalDateTime.now();
        this.rejectedReason = reason;
    }
}
