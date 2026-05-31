package myexpressionfriend_api.game.repository;

import myexpressionfriend_api.common.domain.PeersTheme;
import myexpressionfriend_api.game.domain.DialogueSession;
import myexpressionfriend_api.statistics.dialogue.repository.DialogueBestScoreProjection;
import myexpressionfriend_api.statistics.dialogue.repository.DialogueScore0RateProjection;
import myexpressionfriend_api.statistics.dialogue.repository.WeeklyProgressProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DialogueSessionRepository extends JpaRepository<DialogueSession, UUID> {

    @Query("SELECT COUNT(s) FROM DialogueSession s WHERE s.child.childId = :childId AND s.theme = :theme")
    long countByChildAndTheme(@Param("childId") UUID childId, @Param("theme") PeersTheme theme);

    @Query("SELECT COALESCE(AVG(s.scoreRate), 0) FROM DialogueSession s WHERE s.child.childId = :childId AND s.theme = :theme")
    double avgScoreRateByChildAndTheme(@Param("childId") UUID childId, @Param("theme") PeersTheme theme);

    // native query: theme 값은 DB에 저장된 displayName 문자열로 전달
    @Query(value = """
            SELECT
                COUNT(CASE WHEN t.npc_reaction_expression = 'Joy' THEN 1 END)::float / NULLIF(COUNT(t.turn_id), 0)
            FROM dialogue_sessions s
            JOIN dialogue_turns t ON t.session_id = s.session_id
            WHERE s.child_id = :childId AND s.theme = :theme
            """, nativeQuery = true)
    Double calcRapportIndexByChildAndTheme(@Param("childId") UUID childId, @Param("theme") String theme);

    @Query(value = """
            SELECT ROW_NUMBER() OVER (ORDER BY started_at) AS weekNumber,
                   score_rate AS scoreRate
            FROM dialogue_sessions
            WHERE child_id = :childId AND theme = :theme
            ORDER BY started_at
            """, nativeQuery = true)
    List<WeeklyProgressProjection> findWeeklyProgressByChildAndTheme(
            @Param("childId") UUID childId, @Param("theme") String theme);

    /** 아동의 마지막 대화 게임 플레이 시각 (미접속 체크용) */
    @Query("SELECT MAX(s.startedAt) FROM DialogueSession s WHERE s.child.childId = :childId")
    Optional<Instant> findLastPlayedAt(@Param("childId") UUID childId);

    /** 주간 참여 달성률: 특정 기간의 세션 수 집계 */
    @Query("SELECT COUNT(DISTINCT DATE(s.startedAt)) FROM DialogueSession s WHERE s.child.childId = :childId AND s.startedAt BETWEEN :from AND :to")
    long countSessionDaysBetween(@Param("childId") UUID childId, @Param("from") Instant from, @Param("to") Instant to);

    /** 하이라이트 카드: 개인 최고 scoreRate (현재 세션 제외) */
    @Query("SELECT COALESCE(MAX(s.scoreRate), 0) FROM DialogueSession s WHERE s.child.childId = :childId AND s.sessionId <> :excludeSessionId")
    float findPersonalBestScoreRate(@Param("childId") UUID childId, @Param("excludeSessionId") UUID excludeSessionId);

    /** 주간 통계용: 특정 기간 세션 목록 */
    @Query("SELECT s FROM DialogueSession s WHERE s.child.childId = :childId AND s.startedAt BETWEEN :from AND :to ORDER BY s.startedAt DESC")
    List<DialogueSession> findSessionsBetween(@Param("childId") UUID childId, @Param("from") Instant from, @Param("to") Instant to);

    /** EMA 계산: child+theme 최신 N개 세션 score_rate (startedAt 내림차순) */
    @Query("SELECT s FROM DialogueSession s WHERE s.child.childId = :childId AND s.theme = :theme ORDER BY s.startedAt DESC")
    List<DialogueSession> findRecentByChildAndTheme(@Param("childId") UUID childId, @Param("theme") PeersTheme theme,
                                                    org.springframework.data.domain.Pageable pageable);

    /** α 결정: 최근 4주(28일) 내 child+theme 세션 수 */
    @Query("SELECT COUNT(s) FROM DialogueSession s WHERE s.child.childId = :childId AND s.theme = :theme AND s.startedAt >= :since")
    long countByChildAndThemeSince(@Param("childId") UUID childId, @Param("theme") PeersTheme theme, @Param("since") Instant since);

    @Query("SELECT MIN(s.startedAt) FROM DialogueSession s WHERE s.child.childId = :childId AND s.theme = :theme")
    Optional<Instant> findFirstStartedAtByChildAndTheme(@Param("childId") UUID childId, @Param("theme") PeersTheme theme);

    /** 2-7 주제별 진행도: 플레이한 테마 목록과 세션 수 */
    @Query("SELECT s.theme, COUNT(s) FROM DialogueSession s WHERE s.child.childId = :childId GROUP BY s.theme")
    List<Object[]> countSessionsByTheme(@Param("childId") UUID childId);

    @Query("SELECT s.scenarioId FROM DialogueSession s WHERE s.child.childId = :childId")
    List<String> findScenarioIdsByChildId(@Param("childId") UUID childId);

    // 플레이 기록 조회 (페이지네이션)
    Page<DialogueSession> findByChild_ChildIdOrderByStartedAtDesc(UUID childId, Pageable pageable);

    Page<DialogueSession> findByChild_ChildIdAndThemeOrderByStartedAtDesc(UUID childId, PeersTheme theme, Pageable pageable);

    List<DialogueSession> findByChild_ChildIdOrderByStartedAtAsc(UUID childId);

    /** 하이라이트용: 특정 시각 이전 테마별 최고 scoreRate (배치 조회) */
    @Query("SELECT s.theme AS theme, MAX(s.scoreRate) AS bestScoreRate " +
           "FROM DialogueSession s " +
           "WHERE s.child.childId = :childId AND s.startedAt < :before " +
           "GROUP BY s.theme")
    List<DialogueBestScoreProjection> findBestScoreRatePerThemeBefore(
            @Param("childId") UUID childId, @Param("before") Instant before);

    /** 대시보드 요약용: 아동의 전체 테마별 주간 진행 트렌드를 한 번에 배치 조회 (N+1 방지) */
    @Query(value = """
            SELECT theme                                                                  AS theme,
                   ROW_NUMBER() OVER (PARTITION BY theme ORDER BY started_at)            AS weekNumber,
                   score_rate                                                             AS scoreRate
            FROM dialogue_sessions
            WHERE child_id = :childId
            ORDER BY theme, started_at
            """, nativeQuery = true)
    List<myexpressionfriend_api.statistics.dialogue.repository.DialogueAllThemeProgressProjection>
    findAllWeeklyProgressByChild(@Param("childId") UUID childId);

    /** 2-5 재시도 감소율 Baseline 계산: child+theme의 가장 오래된 N개 세션 */
    @Query("SELECT s FROM DialogueSession s WHERE s.child.childId = :childId AND s.theme = :theme ORDER BY s.startedAt ASC")
    List<DialogueSession> findOldestByChildAndTheme(
            @Param("childId") UUID childId,
            @Param("theme") PeersTheme theme,
            org.springframework.data.domain.Pageable pageable);

    @Query(value = """
            SELECT AVG(score0_rate) AS score0Rate
            FROM (
                SELECT
                    COUNT(*) FILTER (WHERE t.selected_score = 0)::float / NULLIF(COUNT(*), 0) AS score0_rate
                FROM dialogue_sessions s
                JOIN dialogue_turns t ON t.session_id = s.session_id
                WHERE s.child_id = :childId AND s.theme = :theme
                GROUP BY s.session_id, s.started_at
                ORDER BY s.started_at ASC
                LIMIT :limit
            ) session_rates
            """, nativeQuery = true)
    DialogueScore0RateProjection avgOldestScore0RateByChildAndTheme(
            @Param("childId") UUID childId,
            @Param("theme") String theme,
            @Param("limit") int limit);

    @Query(value = """
            SELECT AVG(score0_rate) AS score0Rate
            FROM (
                SELECT
                    COUNT(*) FILTER (WHERE t.selected_score = 0)::float / NULLIF(COUNT(*), 0) AS score0_rate
                FROM dialogue_sessions s
                JOIN dialogue_turns t ON t.session_id = s.session_id
                WHERE s.child_id = :childId AND s.theme = :theme
                GROUP BY s.session_id, s.started_at
                ORDER BY s.started_at DESC
                LIMIT :limit
            ) session_rates
            """, nativeQuery = true)
    DialogueScore0RateProjection avgRecentScore0RateByChildAndTheme(
            @Param("childId") UUID childId,
            @Param("theme") String theme,
            @Param("limit") int limit);
}
