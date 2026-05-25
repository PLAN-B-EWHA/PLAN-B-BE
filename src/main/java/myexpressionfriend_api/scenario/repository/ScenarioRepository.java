package myexpressionfriend_api.scenario.repository;

import myexpressionfriend_api.scenario.domain.Scenario;
import myexpressionfriend_api.scenario.domain.ScenarioApprovalStatus;
import myexpressionfriend_api.game.domain.ScenarioSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScenarioRepository extends JpaRepository<Scenario, String> {

    /** 대화 턴 + 선택지까지 한 번에 로드 (N+1 방지) */
    @Query("""
            SELECT DISTINCT s FROM Scenario s
            LEFT JOIN FETCH s.dialogueFlow t
            WHERE s.scenarioId = :scenarioId
            """)
    Optional<Scenario> findWithFullDetail(@Param("scenarioId") String scenarioId);

    /** 대화 턴 + 선택지(options) 포함 조회 (통계/결과 저장용) */
    @Query("""
            SELECT DISTINCT s FROM Scenario s
            LEFT JOIN FETCH s.dialogueFlow t
            WHERE s.scenarioId = :scenarioId
            """)
    Optional<Scenario> findWithFullDetailAndOptions(@Param("scenarioId") String scenarioId);

    /** 특정 주차의 시나리오 전체를 turns + options 포함 한 번에 로드 */
    @Query("""
            SELECT DISTINCT s FROM Scenario s
            LEFT JOIN FETCH s.dialogueFlow t
            WHERE s.week = :week
            ORDER BY s.scenarioId ASC
            """)
    List<Scenario> findAllByWeekWithFullDetail(@Param("week") Integer week);

    @Query("""
            SELECT DISTINCT s FROM Scenario s
            LEFT JOIN FETCH s.dialogueFlow t
            WHERE s.approvalStatus = :status
              AND s.source IN :sources
            ORDER BY s.updatedAt DESC, s.scenarioId ASC
            """)
    List<Scenario> findAllByStatusAndSourcesWithFullDetail(
            @Param("status") ScenarioApprovalStatus status,
            @Param("sources") List<ScenarioSource> sources);

    @Query("""
            SELECT DISTINCT s FROM Scenario s
            LEFT JOIN FETCH s.dialogueFlow t
            WHERE s.week = :week
              AND s.approvalStatus = :status
              AND s.source IN :sources
            ORDER BY s.scenarioId ASC
            """)
    List<Scenario> findAllByWeekAndStatusAndSourcesWithFullDetail(
            @Param("week") Integer week,
            @Param("status") ScenarioApprovalStatus status,
            @Param("sources") List<ScenarioSource> sources);

    boolean existsByScenarioIdAndSourceAndApprovalStatus(
            String scenarioId,
            ScenarioSource source,
            ScenarioApprovalStatus approvalStatus);

    boolean existsByScenarioId(String scenarioId);
}
