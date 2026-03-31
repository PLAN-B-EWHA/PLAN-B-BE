package myexpressionfriend_api.scenario.repository;

import myexpressionfriend_api.scenario.domain.Scenario;
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

    /** 특정 주차의 시나리오 전체를 turns + options 포함 한 번에 로드 */
    @Query("""
            SELECT DISTINCT s FROM Scenario s
            LEFT JOIN FETCH s.dialogueFlow t
            WHERE s.week = :week
            ORDER BY s.scenarioId ASC
            """)
    List<Scenario> findAllByWeekWithFullDetail(@Param("week") Integer week);

    boolean existsByScenarioId(String scenarioId);
}
