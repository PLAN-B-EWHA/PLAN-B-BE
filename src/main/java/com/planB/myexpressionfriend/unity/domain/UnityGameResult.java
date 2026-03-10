package com.planB.myexpressionfriend.unity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Unity 게임 결과 저장 엔티티
 */
@Entity
@Table(name = "unity_game_results", indexes = {
        @Index(name = "idx_unity_game_results_mission_id", columnList = "mission_id"),
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