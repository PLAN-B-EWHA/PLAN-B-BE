package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.child.ChildrenAuthorizedUser;
import com.planB.myexpressionfriend.common.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ChildrenAuthorizedUser Repository
 */
public interface ChildrenAuthorizedUserRepository extends JpaRepository<ChildrenAuthorizedUser, UUID> {

    /**
     * 아동 + 사용자로 권한 조회
     */
    Optional<ChildrenAuthorizedUser> findByChildAndUser(Child child, User user);

    /**
     * 아동 + 사용자 ID로 권한 조회
     */
    @Query("""
        SELECT au FROM ChildrenAuthorizedUser au
        WHERE au.child.childId = :childId
        AND au.user.userId = :userId
        """)
    Optional<ChildrenAuthorizedUser> findByChildIdAndUserId(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId
    );

    /**
     * 특정 아동의 모든 권한 목록 조회 (활성화된 것만)
     */
    @Query("""
        SELECT au FROM ChildrenAuthorizedUser au
        JOIN FETCH au.user
        WHERE au.child.childId = :childId
        AND au.isActive = true
        ORDER BY au.isPrimary DESC, au.authorizedAt ASC
        """)
    List<ChildrenAuthorizedUser> findActiveByChildId(@Param("childId") UUID childId);

    /**
     * 특정 아동의 주보호자 조회
     */
    @Query("""
        SELECT au FROM ChildrenAuthorizedUser au
        JOIN FETCH au.user
        WHERE au.child.childId = :childId
        AND au.isPrimary = true
        AND au.isActive = true
        """)
    Optional<ChildrenAuthorizedUser> findPrimaryByChildId(@Param("childId") UUID childId);

    /**
     * 특정 사용자가 주보호자인 아동 목록
     */
    @Query("""
        SELECT au FROM ChildrenAuthorizedUser au
        JOIN FETCH au.child
        WHERE au.user.userId = :userId
        AND au.isPrimary = true
        AND au.isActive = true
        """)
    List<ChildrenAuthorizedUser> findPrimaryByUserId(@Param("userId") UUID userId);

    /**
     * 특정 권한을 가진 사용자 목록 조회
     */
    @Query("""
        SELECT au FROM ChildrenAuthorizedUser au
        JOIN FETCH au.user
        WHERE au.child.childId = :childId
        AND au.isActive = true
        AND (:permission MEMBER OF au.permissions OR au.isPrimary = true)
        """)
    List<ChildrenAuthorizedUser> findByChildIdAndPermission(
            @Param("childId") UUID childId,
            @Param("permission") ChildPermissionType permission
    );

    /**
     * 권한 존재 여부 확인
     */
    @Query("""
        SELECT COUNT(au) > 0 FROM ChildrenAuthorizedUser au
        WHERE au.child.childId = :childId
        AND au.user.userId = :userId
        AND au.isActive = true
        AND (:permission MEMBER OF au.permissions OR au.isPrimary = true)
        """)
    boolean existsByChildIdAndUserIdAndPermission(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId,
            @Param("permission") ChildPermissionType permission
    );

    /**
     * 아동 + 사용자 조합 존재 여부 (중복 체크)
     */
    boolean existsByChildAndUser(Child child, User user);

    /**
     * 특정 아동의 주보호자 개수 (1명만 허용)
     */
    @Query("""
        SELECT COUNT(au) FROM ChildrenAuthorizedUser au
        WHERE au.child.childId = :childId
        AND au.isPrimary = true
        AND au.isActive = true
        """)
    long countPrimaryByChildId(@Param("childId") UUID childId);
}