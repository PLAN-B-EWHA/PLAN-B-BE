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
 * 주요 기능:
 * - 권한 기반 미션 조회 (VIEW_REPORT 권한 필요)
 * - 아동별/치료사별 미션 조회
 * - 상태별 필터링
 * - N+1 방지 (JOIN FETCH)
 */
@Repository
public interface AssignedMissionRepository extends JpaRepository<AssignedMission, UUID> {

    // ============= 기본 조회 (권한 검증 포함) =============

    /**
     * 미션 상세 조회 (권한 검증 포함, N+1 방지)
     *
     * @param missionId 미션 ID
     * @param userId 조회 요청 사용자 ID
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
     * 특정 아동의 미션 목록 조회 (권한 검증 포함, 페이징)
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
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

    // ============= 상태별 조회 =============

    /**
     * 특정 아동의 상태별 미션 조회 (권한 검증 포함, 페이징)
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
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

    // ============= 치료사별 조회 =============

    /**
     * 특정 치료사가 할당한 미션 조회 (권한 검증 포함, 페이징)
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
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

    // ============= 날짜 범위별 조회 =============

    /**
     * 날짜 범위별 미션 조회 (권한 검증 포함, 페이징)
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @param pageable 페이징 정보
     * @return Page<AssignedMission>
     */
    @Query("""
        SELECT m FROM AssignedMission m
        JOIN FETCH m.therapist
        JOIN FETCH m.template
        WHERE m.child.childId = :childId
        AND m.assignedAt BETWEEN :startDate AND :endDate
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

    // ============= 마감일 관련 =============

    /**
     * 마감일이 지난 미션 조회 (권한 검증 포함)
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @param now 현재 시간
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

    // ============= 완료 대기중인 미션 =============

    /**
     * 완료 대기중인 미션 조회 (치료사 검증 필요, 권한 검증 포함)
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID (치료사)
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

    // ============= 통계 =============

    /**
     * 특정 아동의 미션 총 개수 (권한 검증 포함)
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @return 미션 개수
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
     * 상태별 미션 개수 (권한 검증 포함)
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @param status 미션 상태
     * @return 미션 개수
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

    // ============= 관리자용 (권한 검증 없음) =============

    /**
     * 특정 아동의 모든 미션 조회 (관리자용, 권한 검증 없음)
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
     * 특정 치료사가 할당한 모든 미션 조회 (관리자용, 권한 검증 없음)
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