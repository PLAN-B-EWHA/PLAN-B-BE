package myexpressionfriend_api.homework.repository;

import myexpressionfriend_api.homework.domain.HomeworkReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HomeworkReportRepository extends JpaRepository<HomeworkReport, UUID> {

    Optional<HomeworkReport> findByHomework_HomeworkId(UUID homeworkId);

    boolean existsByHomework_HomeworkId(UUID homeworkId);

    List<HomeworkReport> findByHomework_Child_ChildId(UUID childId);

    @Query("""
            SELECT r FROM HomeworkReport r
            JOIN FETCH r.homework h
            WHERE h.child.childId = :childId
              AND r.reportedAt >= :from
              AND r.reportedAt < :to
            """)
    List<HomeworkReport> findReportsBetween(
            @Param("childId") UUID childId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
