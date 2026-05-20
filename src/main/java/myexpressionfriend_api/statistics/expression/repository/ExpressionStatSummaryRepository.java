package myexpressionfriend_api.statistics.expression.repository;

import myexpressionfriend_api.statistics.expression.domain.ExpressionStatSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpressionStatSummaryRepository extends JpaRepository<ExpressionStatSummary, UUID> {

    Optional<ExpressionStatSummary> findByChild_ChildIdAndEmotionTarget(UUID childId, String emotionTarget);

    List<ExpressionStatSummary> findByChild_ChildId(UUID childId);
}
