package com.planB.myexpressionfriend.unity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "unity_missions", indexes = {
        @Index(name = "idx_unity_missions_external_id", columnList = "external_mission_id"),
        @Index(name = "idx_unity_missions_type", columnList = "mission_type"),
        @Index(name = "idx_unity_missions_created_at", columnList = "created_at")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@EntityListeners(AuditingEntityListener.class)
public class UnityMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unity_mission_id", updatable = false, nullable = false)
    private Long unityMissionId;

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

    @OneToOne(mappedBy = "mission")
    private UnityExpressionMissionDetail expressionDetail;

    @OneToOne(mappedBy = "mission")
    private UnitySituationMissionDetail situationDetail;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}