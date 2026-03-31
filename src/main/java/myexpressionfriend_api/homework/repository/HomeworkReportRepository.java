package myexpressionfriend_api.homework.repository;

import myexpressionfriend_api.homework.domain.HomeworkReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HomeworkReportRepository extends JpaRepository<HomeworkReport, UUID> {

    Optional<HomeworkReport> findByHomework_HomeworkId(UUID homeworkId);

    boolean existsByHomework_HomeworkId(UUID homeworkId);
}
