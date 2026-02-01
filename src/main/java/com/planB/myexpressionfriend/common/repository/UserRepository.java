package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // ============= 기본 조회 메서드 =============

    /**
     * 이메일로 사용자 찾기
     * @param email 사용자 이메일
     * @return User Optional
     */
    Optional<User> findByEmail(String email);

    /**
     * 이메일 존재 여부 확인
     * @param email 확인할 이메일
     * @return 존재 여부
     */
    boolean existsByEmail(String email);

    /**
     * 이름으로 사용자 검색 (부분 일치)
     * @param name 검색할 이름
     * @return 사용자 목록
     */
    List<User> findByNameContaining(String name);


    // ============= 역할 관련 조회 =============

    /**
     * 특정 역할을 가진 사용자 목록 조회
     * @param role 사용자 역할
     * @return 사용자 목록
     */
    List<User> findByRolesContains(UserRole role);

    /**
     * 치료사 목록 조회
     * @return 치료사 목록
     */
    @Query("SELECT u FROM User u WHERE :role MEMBER OF u.roles")
    List<User> findAllByRole(@Param("role") UserRole role);


    // ============= N+1 문제 방지 (roles EAGER 로딩) =============

    /**
     * 이메일로 사용자 찾기 (역할 함께 로딩)
     * Spring Security 인증 시 사용
     * @param email 사용자 이메일
     * @return User Optional (roles 포함)
     */
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    /**
     * ID로 사용자 찾기 (역할 함께 로딩)
     * @param userId 사용자 ID
     * @return User Optional (roles 포함)
     */
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM User u WHERE u.userId = :userId")
    Optional<User> findByIdWithRoles(@Param("userId") UUID userId);

    /**
     * 모든 사용자 조회 (역할 함께 로딩)
     * 이후 속도가 느려진다면 Paging 도입 예정
     * @return 전체 사용자 목록 (roles 포함)
     */
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM User u")
    List<User> findAllWithRoles();


    // ============= 통계 및 관리 =============

    /**
     * 역할별 사용자 수 조회
     * @param role 사용자 역할
     * @return 사용자 수
     */
    @Query("SELECT COUNT(u) FROM User u WHERE :role MEMBER OF u.roles")
    long countByRole(@Param("role") UserRole role);

    /**
     * 특정 기간 내 가입한 사용자 조회
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 사용자 목록
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findByCreatedAtBetween(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate
    );

}
