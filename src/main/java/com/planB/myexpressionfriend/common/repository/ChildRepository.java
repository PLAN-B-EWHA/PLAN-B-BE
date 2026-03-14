package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ExpressionTag;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Child Repository
 *
 * Soft Delete가 적용되어 기본 조회에서는 isDeleted=false 데이터만 조회됩니다.
 */
public interface ChildRepository extends JpaRepository<Child, UUID> {

    /**
     * 게임 세션 생성 시 동시성 제어용 비관적 락 조회 (SELECT FOR UPDATE).
     * createSession 흐름에서만 사용하며, 반드시 쓰기 트랜잭션 안에서 호출해야 합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Child c WHERE c.childId = :childId")
    Optional<Child> findByIdForUpdate(@Param("childId") UUID childId);

    /**
     * 주보호자 기준 아동 목록 조회 (N+1 방지)
     */
    @Query("""
        SELECT DISTINCT c FROM Child c
        JOIN FETCH c.authorizedUsers au
        WHERE au.user.userId = :userId
        AND au.isPrimary = true
        AND au.isActive = true
        """)
    List<Child> findByPrimaryParentUserId(@Param("userId") UUID userId);

    /**
     * 특정 사용자가 접근 가능한 아동 목록 조회
     * (주보호자 + 권한 부여된 사용자)
     */
    @Query("""
        SELECT DISTINCT c FROM Child c
        JOIN FETCH c.authorizedUsers au
        WHERE au.user.userId = :userId
        AND au.isActive = true
        """)
    List<Child> findAccessibleByUserId(@Param("userId") UUID userId);

    /**
     * 아동 상세 조회 (권한 사용자 목록 포함, N+1 방지)
     */
    @Query("""
        SELECT c FROM Child c
        LEFT JOIN FETCH c.authorizedUsers au
        WHERE c.childId = :childId
        """)
    Optional<Child> findByIdWithAuthorizedUsers(@Param("childId") UUID childId);

    /**
     * 주보호자 존재 여부 확인
     */
    @Query("""
        SELECT COUNT(c) > 0 FROM Child c
        JOIN c.authorizedUsers au
        WHERE c.childId = :childId
        AND au.isPrimary = true
        AND au.isActive = true
        """)
    boolean hasPrimaryParent(@Param("childId") UUID childId);

    /**
     * 특정 사용자가 주보호자인 아동 수 조회 (제한 체크용)
     */
    @Query("""
        SELECT COUNT(c) FROM Child c
        JOIN c.authorizedUsers au
        WHERE au.user.userId = :userId
        AND au.isPrimary = true
        AND au.isActive = true
        """)
    long countByPrimaryParentUserId(@Param("userId") UUID userId);

    @Query("""
        SELECT DISTINCT c FROM Child c
        JOIN c.preferredExpressions pe
        WHERE pe = :expressionTag
        """)
    List<Child> findByPreferredExpression(@Param("expressionTag") ExpressionTag expressionTag);

    @Query("""
        SELECT DISTINCT c FROM Child c
        JOIN c.difficultExpressions de
        WHERE de = :expressionTag
        """)
    List<Child> findByDifficultExpression(@Param("expressionTag") ExpressionTag expressionTag);
}
