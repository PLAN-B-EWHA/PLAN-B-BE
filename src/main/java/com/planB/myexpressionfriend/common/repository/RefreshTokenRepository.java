package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.token.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * RefreshToken Repository
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    /**
     * User ID로 Refresh Token 조회
     *
     * @param userId 사용자 ID
     * @return RefreshToken Optional
     */
    Optional<RefreshToken> findByUserId(String userId);

    /**
     * User ID로 Refresh Token 삭제
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(String userId);

    /**
     * User ID로 Refresh Token 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @return 존재하면 true
     */
    boolean existsByUserId(String userId);

    /**
     * 만료된 Refresh Token 일괄 삭제
     *
     * 배치 작업으로 주기적으로 실행 권장 (예: 매일 자정)
     *
     * @param now 현재 시간
     * @return 삭제된 레코드 수
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * 특정 시간 이전에 생성된 토큰 삭제
     *
     * 오래된 토큰 정리용
     *
     * @param cutoffTime 기준 시간
     * @return 삭제된 레코드 수
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.createdAt < :cutoffTime")
    int deleteOldTokens(@Param("cutoffTime") LocalDateTime cutoffTime);
}