package myexpressionfriend_api.game.repository;

import myexpressionfriend_api.common.domain.PeersTheme;
import myexpressionfriend_api.game.domain.DialogueTurn;
import myexpressionfriend_api.statistics.dialogue.errorpattern.repository.ZeroScoreTurnProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DialogueTurnRepository extends JpaRepository<DialogueTurn, UUID> {

    List<DialogueTurn> findBySession_SessionIdInOrderByTurnNumber(List<UUID> sessionIds);

    /** 2-2 선택 편향 분석: child+theme의 0점 턴 전체 조회 */
    @Query("""
            SELECT t FROM DialogueTurn t
            JOIN t.session s
            WHERE s.child.childId = :childId AND s.theme = :theme AND t.selectedScore = 0
            """)
    List<DialogueTurn> findZeroScoreTurnsByChildAndTheme(
            @Param("childId") UUID childId,
            @Param("theme") PeersTheme theme);

    @Query("""
        SELECT
            t.turnId AS turnId,
            t.child.childId AS childId,
            s.theme AS theme,
            s.scenarioId AS scenarioId,
            t.turnNumber AS turnNumber,
            t.selectedOptionOrder AS selectedOptionOrder,
            s.startedAt AS startedAt
        FROM DialogueTurn t
        JOIN t.session s
        WHERE t.child.childId = :childId
          AND t.selectedScore = 0
        ORDER BY s.startedAt DESC
        """)
    List<ZeroScoreTurnProjection> findLatestZeroScoreTurns(@Param("childId") UUID childId);
}
