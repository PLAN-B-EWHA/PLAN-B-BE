package myexpressionfriend_api.statistics.dialogue.errorpattern.repository;

import myexpressionfriend_api.common.domain.PeersTheme;
import myexpressionfriend_api.statistics.dialogue.errorpattern.domain.DialogueErrorPatternSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface DialogueErrorPatternSummaryRepository extends JpaRepository<DialogueErrorPatternSummary, UUID> {
    Optional<DialogueErrorPatternSummary> findByChild_ChildIdAndTheme(UUID childId, PeersTheme theme);

    /** isInCooldown: findAll() 대신 EXISTS 쿼리로 최적화 */
    @Query("""
            SELECT COUNT(s) > 0 FROM DialogueErrorPatternSummary s
            WHERE s.child.childId = :childId AND s.lastRefreshedAt > :threshold
            """)
    boolean existsRecentRefreshByChild(
            @Param("childId") UUID childId,
            @Param("threshold") LocalDateTime threshold);
}
