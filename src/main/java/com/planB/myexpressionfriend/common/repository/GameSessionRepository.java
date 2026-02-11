package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.game.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {

    /**
     * 세션 토큰으로 조회
     */
    Optional<GameSession> findBySessionToken(String sessionToken);

    /**
     * 유효한 세션 토큰으로 조회
     */
    @Query("""
        SELECT gs FROM GameSession gs
        WHERE gs.sessionToken = :sessionToken
        AND gs.isActive = true
        AND gs.expiresAt > :now
        """)
    Optional<GameSession> findValidSessionByToken(
            @Param("sessionToken") String sessionToken,
            @Param("now") LocalDateTime now
    );

    /**
     * 아동의 활성 세션 목록
     */
    @Query("""
        SELECT gs FROM GameSession gs
        WHERE gs.child.childId = :childId
        AND gs.isActive = true
        AND gs.expiresAt > :now
        ORDER BY gs.createdAt DESC
        """)
    List<GameSession> findActiveSessionsByChildId(
            @Param("childId") UUID childId,
            @Param("now") LocalDateTime now
    );

    /**
     * 아동의 모든 활성 세션 종료
     */
    @Modifying
    @Query("""
        UPDATE GameSession gs
        SET gs.isActive = false
        WHERE gs.child.childId = :childId
        AND gs.isActive = true
        """)
    void terminateAllSessionsByChildId(@Param("childId") UUID childId);

    /**
     * 만료된 세션 삭제 (배치용)
     */
    @Modifying
    @Query("""
        DELETE FROM GameSession gs
        WHERE gs.expiresAt < :cutoffTime
        """)
    void deleteExpiredSessions(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 사용자의 모든 활성 세션 조회
     */
    @Query("""
        SELECT gs FROM GameSession gs
        WHERE gs.authenticatedBy.userId = :userId
        AND gs.isActive = true
        AND gs.expiresAt > :now
        ORDER BY gs.createdAt DESC
        """)
    List<GameSession> findActiveSessionsByUserId(
            @Param("userId") UUID userId,
            @Param("now") LocalDateTime now
    );
}
