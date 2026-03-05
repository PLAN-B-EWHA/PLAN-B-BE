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
 * AssignedMission Repository
 *
 */
@Repository
public interface AssignedMissionRepository extends JpaRepository<AssignedMission, UUID> {


    /**
     *
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
     *
     * @param childId ?꾨룞 ID
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
     *
     * @param childId ?꾨룞 ID
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
     *
     * @param childId ?꾨룞 ID
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
     *
     * @param childId ?꾨룞 ID
     * @param startDate ?쒖옉?쇱떆
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
     *
     * @param childId ?꾨룞 ID
     * @param now ?꾩옱 ?쒓컙
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
     *
     * @param childId ?꾨룞 ID
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

    // ============= ?듦퀎 =============

    /**
     *
     * @param childId ?꾨룞 ID
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
     *
     * @param childId ?꾨룞 ID
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
     *
     * @param childId ?꾨룞 ID
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
     *
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
