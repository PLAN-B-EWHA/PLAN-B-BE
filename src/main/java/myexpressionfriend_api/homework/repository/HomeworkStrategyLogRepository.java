package myexpressionfriend_api.homework.repository;

import myexpressionfriend_api.homework.domain.HomeworkStrategyLog;
import myexpressionfriend_api.homework.domain.StrategyFocus;
import myexpressionfriend_api.homework.domain.StrategyOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface HomeworkStrategyLogRepository extends JpaRepository<HomeworkStrategyLog, UUID> {

    List<HomeworkStrategyLog> findByChild_ChildIdAndStrategyOrderByCreatedAtDesc(
            UUID childId, StrategyFocus strategy);

    /** 아동별 전략별 성공률 집계 */
    @Query("""
            SELECT l.outcome, COUNT(l) FROM HomeworkStrategyLog l
            WHERE l.child.childId = :childId
              AND l.strategy = :strategy
            GROUP BY l.outcome
            """)
    List<Object[]> countOutcomeByChildAndStrategy(@Param("childId") UUID childId,
                                                   @Param("strategy") StrategyFocus strategy);

    /** 가장 최근 시도 횟수 (다음 attemptCount 계산용) */
    @Query("""
            SELECT COALESCE(MAX(l.attemptCount), 0) FROM HomeworkStrategyLog l
            WHERE l.child.childId = :childId
              AND l.strategy = :strategy
            """)
    int findMaxAttemptCount(@Param("childId") UUID childId,
                            @Param("strategy") StrategyFocus strategy);
}
