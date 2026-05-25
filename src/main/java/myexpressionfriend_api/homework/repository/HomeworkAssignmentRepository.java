package myexpressionfriend_api.homework.repository;

import myexpressionfriend_api.homework.domain.HomeworkAssignment;
import myexpressionfriend_api.homework.domain.HomeworkStatus;
import myexpressionfriend_api.homework.domain.StrategyFocus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HomeworkAssignmentRepository extends JpaRepository<HomeworkAssignment, UUID> {

    Page<HomeworkAssignment> findByChild_ChildIdOrderByCreatedAtDesc(UUID childId, Pageable pageable);

    List<HomeworkAssignment> findByChild_ChildId(UUID childId);

    Page<HomeworkAssignment> findByChild_ChildIdAndStatusOrderByCreatedAtDesc(
            UUID childId, HomeworkStatus status, Pageable pageable);

    List<HomeworkAssignment> findByChild_ChildIdAndWeek(UUID childId, Integer week);

    List<HomeworkAssignment> findByChild_ChildIdAndStatusOrderByDueDateAscCreatedAtDesc(
            UUID childId, HomeworkStatus status);

    /** 아동별 전략 누적 시도 횟수 (StrategyLog attempt_count 계산용) */
    @Query("""
            SELECT COUNT(h) FROM HomeworkAssignment h
            WHERE h.child.childId = :childId
              AND h.strategyFocus = :strategy
            """)
    long countByChildIdAndStrategy(@Param("childId") UUID childId,
                                   @Param("strategy") StrategyFocus strategy);

    Optional<HomeworkAssignment> findByHomeworkIdAndChild_ChildId(UUID homeworkId, UUID childId);
}
