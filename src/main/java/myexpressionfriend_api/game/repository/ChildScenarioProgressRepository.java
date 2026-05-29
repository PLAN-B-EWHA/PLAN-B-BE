package myexpressionfriend_api.game.repository;

import myexpressionfriend_api.game.domain.ChildScenarioProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChildScenarioProgressRepository extends JpaRepository<ChildScenarioProgress, UUID> {

    Optional<ChildScenarioProgress> findByChild_ChildIdAndScenarioId(UUID childId, String scenarioId);

    @Query("""
            SELECT p.scenarioId FROM ChildScenarioProgress p
            WHERE p.child.childId = :childId
              AND p.scenarioId IN :scenarioIds
            """)
    List<String> findCompletedScenarioIds(
            @Param("childId") UUID childId,
            @Param("scenarioIds") List<String> scenarioIds);
}
