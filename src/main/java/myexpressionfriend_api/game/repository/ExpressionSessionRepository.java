package myexpressionfriend_api.game.repository;

import myexpressionfriend_api.game.domain.ExpressionSession;
import myexpressionfriend_api.statistics.expression.repository.ExpressionBestAccuracyProjection;
import myexpressionfriend_api.statistics.expression.repository.ExpressionDurationAverageProjection;
import myexpressionfriend_api.statistics.expression.repository.ExpressionSessionAggregateProjection;
import myexpressionfriend_api.statistics.expression.repository.ExpressionSessionTrendProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpressionSessionRepository extends JpaRepository<ExpressionSession, UUID> {

    @Query("SELECT COUNT(s) FROM ExpressionSession s WHERE s.child.childId = :childId AND s.emotionTarget = :emotion")
    long countByChildAndEmotion(@Param("childId") UUID childId, @Param("emotion") String emotion);

    @Query("SELECT COUNT(s) FROM ExpressionSession s WHERE s.child.childId = :childId AND s.emotionTarget = :emotion AND s.isSuccess = true")
    long countSuccessByChildAndEmotion(@Param("childId") UUID childId, @Param("emotion") String emotion);

    @Query("SELECT COALESCE(AVG(s.totalTries), 0) FROM ExpressionSession s WHERE s.child.childId = :childId AND s.emotionTarget = :emotion")
    double avgTriesByChildAndEmotion(@Param("childId") UUID childId, @Param("emotion") String emotion);

    @Query(value = """
            SELECT ROW_NUMBER() OVER (ORDER BY started_at) AS sessionNumber,
                   final_accuracy                          AS finalAccuracy,
                   is_success                              AS isSuccess
            FROM expression_sessions
            WHERE child_id = :childId AND emotion_target = :emotion
            ORDER BY started_at
            """, nativeQuery = true)
    List<ExpressionSessionTrendProjection> findSessionTrendByChildAndEmotion(
            @Param("childId") UUID childId, @Param("emotion") String emotion);

    /** upsertForSession용: child+emotion 전체 세션 (시작시간 오름차순) */
    @Query("SELECT s FROM ExpressionSession s WHERE s.child.childId = :childId AND s.emotionTarget = :emotion ORDER BY s.startedAt ASC")
    List<ExpressionSession> findAllByChildIdAndEmotionOrderByStartedAt(
            @Param("childId") UUID childId, @Param("emotion") String emotion);

    @Query(value = """
            SELECT
                COUNT(*)::int AS sessionCount,
                COUNT(*) FILTER (WHERE is_success = true) AS successCount,
                COALESCE(AVG(COALESCE(total_tries, 1)), 0) AS avgRetry,
                COUNT(*) FILTER (
                    WHERE EXTRACT(EPOCH FROM (ended_at - started_at)) BETWEEN :minSec AND :maxSec
                )::int AS validSessionCount,
                AVG(EXTRACT(EPOCH FROM (ended_at - started_at))) FILTER (
                    WHERE EXTRACT(EPOCH FROM (ended_at - started_at)) BETWEEN :minSec AND :maxSec
                ) AS avgSessionDurationSec
            FROM expression_sessions
            WHERE child_id = :childId AND emotion_target = :emotion
            """, nativeQuery = true)
    ExpressionSessionAggregateProjection aggregateByChildAndEmotion(
            @Param("childId") UUID childId,
            @Param("emotion") String emotion,
            @Param("minSec") int minSec,
            @Param("maxSec") int maxSec);

    @Query(value = """
            SELECT AVG(duration_sec) AS avgDurationSec
            FROM (
                SELECT EXTRACT(EPOCH FROM (ended_at - started_at)) AS duration_sec
                FROM expression_sessions
                WHERE child_id = :childId
                  AND emotion_target = :emotion
                  AND EXTRACT(EPOCH FROM (ended_at - started_at)) BETWEEN :minSec AND :maxSec
                ORDER BY started_at ASC
                LIMIT :limit
            ) durations
            """, nativeQuery = true)
    ExpressionDurationAverageProjection avgOldestValidDurationByChildAndEmotion(
            @Param("childId") UUID childId,
            @Param("emotion") String emotion,
            @Param("minSec") int minSec,
            @Param("maxSec") int maxSec,
            @Param("limit") int limit);

    @Query(value = """
            SELECT AVG(duration_sec) AS avgDurationSec
            FROM (
                SELECT EXTRACT(EPOCH FROM (ended_at - started_at)) AS duration_sec
                FROM expression_sessions
                WHERE child_id = :childId
                  AND emotion_target = :emotion
                  AND EXTRACT(EPOCH FROM (ended_at - started_at)) BETWEEN :minSec AND :maxSec
                ORDER BY started_at DESC
                LIMIT :limit
            ) durations
            """, nativeQuery = true)
    ExpressionDurationAverageProjection avgRecentValidDurationByChildAndEmotion(
            @Param("childId") UUID childId,
            @Param("emotion") String emotion,
            @Param("minSec") int minSec,
            @Param("maxSec") int maxSec,
            @Param("limit") int limit);

    @Query("SELECT s FROM ExpressionSession s WHERE s.child.childId = :childId AND s.emotionTarget = :emotion ORDER BY s.startedAt ASC")
    List<ExpressionSession> findOldestByChildAndEmotion(
            @Param("childId") UUID childId,
            @Param("emotion") String emotion,
            Pageable pageable);

    @Query("SELECT s FROM ExpressionSession s WHERE s.child.childId = :childId AND s.emotionTarget = :emotion ORDER BY s.startedAt DESC")
    List<ExpressionSession> findRecentByChildAndEmotion(
            @Param("childId") UUID childId,
            @Param("emotion") String emotion,
            Pageable pageable);

    /** 주간 참여 달성률: 특정 기간의 세션 수 집계 */
    @Query("SELECT COUNT(DISTINCT DATE(s.startedAt)) FROM ExpressionSession s WHERE s.child.childId = :childId AND s.startedAt BETWEEN :from AND :to")
    long countSessionDaysBetween(@Param("childId") UUID childId, @Param("from") Instant from, @Param("to") Instant to);

    /** 하이라이트 카드: 특정 감정의 개인 최고 accuracy (현재 세션 제외) */
    @Query("SELECT COALESCE(MAX(s.finalAccuracy), 0) FROM ExpressionSession s WHERE s.child.childId = :childId AND s.emotionTarget = :emotion AND s.sessionId <> :excludeSessionId")
    float findPersonalBestAccuracy(@Param("childId") UUID childId, @Param("emotion") String emotion, @Param("excludeSessionId") UUID excludeSessionId);

    /** 아동의 마지막 표정 게임 플레이 시각 (미접속 체크용) */
    @Query("SELECT MAX(s.startedAt) FROM ExpressionSession s WHERE s.child.childId = :childId")
    Optional<Instant> findLastPlayedAt(@Param("childId") UUID childId);

    /** 주간 통계용: 특정 기간 세션 목록 */
    @Query("SELECT s FROM ExpressionSession s WHERE s.child.childId = :childId AND s.startedAt BETWEEN :from AND :to ORDER BY s.startedAt DESC")
    List<ExpressionSession> findSessionsBetween(@Param("childId") UUID childId, @Param("from") Instant from, @Param("to") Instant to);

    // 플레이 기록 조회 (페이지네이션)
    Page<ExpressionSession> findByChild_ChildIdOrderByStartedAtDesc(UUID childId, Pageable pageable);

    Page<ExpressionSession> findByChild_ChildIdAndEmotionTargetOrderByStartedAtDesc(UUID childId, String emotionTarget, Pageable pageable);

    /** 하이라이트용: 특정 시각 이전 감정별 최고 accuracy (배치 조회) */
    @Query("SELECT s.emotionTarget AS emotionTarget, MAX(s.finalAccuracy) AS bestAccuracy " +
           "FROM ExpressionSession s " +
           "WHERE s.child.childId = :childId AND s.startedAt < :before " +
           "GROUP BY s.emotionTarget")
    List<ExpressionBestAccuracyProjection> findBestAccuracyPerEmotionBefore(
            @Param("childId") UUID childId, @Param("before") Instant before);

    /** 대시보드 요약용: 아동의 전체 감정별 세션 트렌드를 한 번에 배치 조회 (N+1 방지) */
    @Query(value = """
            SELECT emotion_target                                                                AS emotionTarget,
                   ROW_NUMBER() OVER (PARTITION BY emotion_target ORDER BY started_at)          AS sessionNumber,
                   final_accuracy                                                               AS finalAccuracy,
                   is_success                                                                   AS isSuccess
            FROM expression_sessions
            WHERE child_id = :childId
            ORDER BY emotion_target, started_at
            """, nativeQuery = true)
    List<myexpressionfriend_api.statistics.expression.repository.ExpressionAllEmotionTrendProjection>
    findAllSessionTrendsByChild(@Param("childId") UUID childId);
}
