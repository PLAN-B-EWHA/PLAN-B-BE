package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.mission.AssignedMission;
import com.planB.myexpressionfriend.common.domain.mission.MissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 미션 배정 저장소
 */
@Repository
public interface AssignedMissionRepository extends JpaRepository<AssignedMission, UUID> {

    /**
     * 권한 검증을 포함한 미션 단건 조회
     *
     * @param missionId 미션 ID
     * @param userId 사용자 ID
     * @return Optional<AssignedMission>
     */
    @Query("""
        SELECT m FROM AssignedMission m
        JOIN FETCH m.child c
        JOIN FETCH m.therapist
        JOIN FETCH m.template
        LEFT JOIN FETCH m.photos
        WHERE m.missionId = :missionId
        AND EXISTS (
            SELECT 1 FROM ChildrenAuthorizedUser au
            WHERE au.child.childId = c.childId
            AND au.user.userId = :userId
            AND au.isActive = true
            AND (
                au.isPrimary = true
                OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
            )
        )
        """)
    Optional<AssignedMission> findByIdWithAuth(
            @Param("missionId") UUID missionId,
            @Param("userId") UUID userId
    );

    /**
     * 권한 검증을 포함한 아동별 미션 목록 조회
     *
     * @param childId 아동 ID
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return Page<AssignedMission>
     */
    @Query("""
        SELECT m FROM AssignedMission m
        JOIN FETCH m.therapist
        JOIN FETCH m.template
        WHERE m.child.childId = :childId
        AND EXISTS (
            SELECT 1 FROM ChildrenAuthorizedUser au
            WHERE au.child.childId = :childId
            AND au.user.userId = :userId
            AND au.isActive = true
            AND (
                au.isPrimary = true
                OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
            )
        )
        """)
    Page<AssignedMission> findByChildIdWithAuth(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId,
            Pageable pageable
    );

    /**
     * 권한 검증을 포함한 상태별 미션 조회
     *
     * @param childId 아동 ID
     * @param userId 사용자 ID
     * @param status 미션 상태
     * @param pageable 페이징 정보
     * @return Page<AssignedMission>
     */
    @Query("""
        SELECT m FROM AssignedMission m
        JOIN FETCH m.therapist
        JOIN FETCH m.template
        WHERE m.child.childId = :childId
        AND m.status = :status
        AND EXISTS (
            SELECT 1 FROM ChildrenAuthorizedUser au
            WHERE au.child.childId = :childId
            AND au.user.userId = :userId
            AND au.isActive = true
            AND (
                au.isPrimary = true
                OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
            )
        )
        """)
    Page<AssignedMission> findByChildIdAndStatusWithAuth(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId,
            @Param("status") MissionStatus status,
            Pageable pageable
    );

    /**
     * 권한 검증을 포함한 치료사별 미션 조회
     *
     * @param childId 아동 ID
     * @param userId 사용자 ID
     * @param therapistId 치료사 ID
     * @param pageable 페이징 정보
     * @return Page<AssignedMission>
     */
    @Query("""
        SELECT m FROM AssignedMission m
        JOIN FETCH m.therapist
        JOIN FETCH m.template
        WHERE m.child.childId = :childId
        AND m.therapist.userId = :therapistId
        AND EXISTS (
            SELECT 1 FROM ChildrenAuthorizedUser au
            WHERE au.child.childId = :childId
            AND au.user.userId = :userId
            AND au.isActive = true
            AND (
                au.isPrimary = true
                OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
            )
        )
        """)
    Page<AssignedMission> findByChildIdAndTherapistWithAuth(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId,
            @Param("therapistId") UUID therapistId,
            Pageable pageable
    );

    /**
     * 권한 검증을 포함한 날짜 범위별 미션 조회
     *
     * @param childId 아동 ID
     * @param userId 사용자 ID
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @param pageable 페이징 정보
     * @return Page<AssignedMission>
     */
    @Query("""
        SELECT m FROM AssignedMission m
        JOIN FETCH m.therapist
        JOIN FETCH m.template
        WHERE m.child.childId = :childId
        AND (
            (m.dueDate IS NOT NULL AND m.dueDate BETWEEN :startDate AND :endDate)
            OR
            (m.dueDate IS NULL AND m.assignedAt BETWEEN :startDate AND :endDate)
        )
        AND EXISTS (
            SELECT 1 FROM ChildrenAuthorizedUser au
            WHERE au.child.childId = :childId
            AND au.user.userId = :userId
            AND au.isActive = true
            AND (
                au.isPrimary = true
                OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
            )
        )
        """)
    Page<AssignedMission> findByChildIdAndDateRangeWithAuth(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 권한 검증을 포함한 기한 초과 미션 조회
     *
     * @param childId 아동 ID
     * @param userId 사용자 ID
     * @param now 현재 시각
     * @param pageable 페이징 정보
     * @return Page<AssignedMission>
     */
    @Query("""
        SELECT m FROM AssignedMission m
        JOIN FETCH m.therapist
        JOIN FETCH m.template
        WHERE m.child.childId = :childId
        AND m.dueDate IS NOT NULL
        AND m.dueDate < :now
        AND m.status NOT IN ('COMPLETED', 'VERIFIED')
        AND EXISTS (
            SELECT 1 FROM ChildrenAuthorizedUser au
            WHERE au.child.childId = :childId
            AND au.user.userId = :userId
            AND au.isActive = true
            AND (
                au.isPrimary = true
                OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
            )
        )
        """)
    Page<AssignedMission> findOverdueMissionsWithAuth(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    /**
     * 권한 검증을 포함한 검토 대기 미션 조회
     *
     * @param childId 아동 ID
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return Page<AssignedMission>
     */
    @Query("""
        SELECT m FROM AssignedMission m
        JOIN FETCH m.therapist
        JOIN FETCH m.template
        LEFT JOIN FETCH m.photos
        WHERE m.child.childId = :childId
        AND m.status = 'COMPLETED'
        AND EXISTS (
            SELECT 1 FROM ChildrenAuthorizedUser au
            WHERE au.child.childId = :childId
            AND au.user.userId = :userId
            AND au.isActive = true
            AND (
                au.isPrimary = true
                OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
            )
        )
        ORDER BY m.completedAt DESC
        """)
    Page<AssignedMission> findPendingVerificationWithAuth(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId,
            Pageable pageable
    );

    /**
     * 치료사 기준 검토 대기 목록 조회
     *
     * @param therapistId 치료사 ID
     * @param childId 아동 ID
     * @param pageable 페이징 정보
     * @return Page<AssignedMission>
     */
    @Query("""
        SELECT m FROM AssignedMission m
        WHERE m.therapist.userId = :therapistId
        AND m.status = 'COMPLETED'
        AND (:childId IS NULL OR m.child.childId = :childId)
        ORDER BY m.completedAt DESC, m.createdAt DESC
        """)
    Page<AssignedMission> findReviewQueueByTherapist(
            @Param("therapistId") UUID therapistId,
            @Param("childId") UUID childId,
            Pageable pageable
    );

    /**
     * 권한 검증을 포함한 아동별 전체 미션 수 조회
     *
     * @param childId 아동 ID
     * @param userId 사용자 ID
     * @return long
     */
    @Query("""
        SELECT COUNT(m) FROM AssignedMission m
        WHERE m.child.childId = :childId
        AND EXISTS (
            SELECT 1 FROM ChildrenAuthorizedUser au
            WHERE au.child.childId = :childId
            AND au.user.userId = :userId
            AND au.isActive = true
            AND (
                au.isPrimary = true
                OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
            )
        )
        """)
    long countByChildIdWithAuth(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId
    );

    /**
     * 권한 검증을 포함한 상태별 미션 수 조회
     *
     * @param childId 아동 ID
     * @param userId 사용자 ID
     * @param status 미션 상태
     * @return long
     */
    @Query("""
        SELECT COUNT(m) FROM AssignedMission m
        WHERE m.child.childId = :childId
        AND m.status = :status
        AND EXISTS (
            SELECT 1 FROM ChildrenAuthorizedUser au
            WHERE au.child.childId = :childId
            AND au.user.userId = :userId
            AND au.isActive = true
            AND (
                au.isPrimary = true
                OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
            )
        )
        """)
    long countByChildIdAndStatusWithAuth(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId,
            @Param("status") MissionStatus status
    );

    /**
     * 아동별 전체 미션 조회
     *
     * @param childId 아동 ID
     * @return List<AssignedMission>
     */
    @Query("""
        SELECT m FROM AssignedMission m
        JOIN FETCH m.therapist
        JOIN FETCH m.template
        WHERE m.child.childId = :childId
        ORDER BY m.assignedAt DESC
        """)
    List<AssignedMission> findAllByChildId(@Param("childId") UUID childId);

    /**
     * 치료사별 전체 미션 조회
     *
     * @param therapistId 치료사 ID
     * @return List<AssignedMission>
     */
    @Query("""
        SELECT m FROM AssignedMission m
        JOIN FETCH m.child
        JOIN FETCH m.template
        WHERE m.therapist.userId = :therapistId
        ORDER BY m.assignedAt DESC
        """)
    List<AssignedMission> findAllByTherapistId(@Param("therapistId") UUID therapistId);
}