package myexpressionfriend_api.statistics.dashboard.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.homework.domain.CompletionStatus;
import myexpressionfriend_api.homework.domain.HomeworkAssignment;
import myexpressionfriend_api.homework.domain.HomeworkReport;
import myexpressionfriend_api.homework.repository.HomeworkAssignmentRepository;
import myexpressionfriend_api.homework.repository.HomeworkReportRepository;
import myexpressionfriend_api.statistics.dashboard.dto.DialogueProgressDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfflineMissionStatisticsService {

    private final HomeworkAssignmentRepository homeworkAssignmentRepository;
    private final HomeworkReportRepository homeworkReportRepository;

    @Transactional(readOnly = true)
    public List<HomeworkReport> findReportsBetween(UUID childId, LocalDateTime from, LocalDateTime to) {
        return homeworkReportRepository.findReportsBetween(childId, from, to);
    }

    @Transactional(readOnly = true)
    public Map<Integer, DialogueProgressDto.OfflineMissionProgressDto> getProgressByWeek(UUID childId) {
        List<HomeworkAssignment> assignments = homeworkAssignmentRepository.findByChild_ChildId(childId);
        List<HomeworkReport> reports = homeworkReportRepository.findByHomework_Child_ChildId(childId);

        Map<UUID, HomeworkReport> reportByHomeworkId = reports.stream()
                .collect(Collectors.toMap(
                        report -> report.getHomework().getHomeworkId(),
                        report -> report,
                        (first, ignored) -> first));

        Map<Integer, MutableOfflineMissionProgress> progressByWeek = new HashMap<>();
        for (HomeworkAssignment assignment : assignments) {
            int week = assignment.getWeek();
            MutableOfflineMissionProgress progress = progressByWeek.computeIfAbsent(
                    week, ignored -> new MutableOfflineMissionProgress());
            progress.assignedCount++;

            HomeworkReport report = reportByHomeworkId.get(assignment.getHomeworkId());
            if (report == null) {
                continue;
            }

            progress.submittedCount++;
            if (Boolean.TRUE.equals(report.getSpontaneousFlag())) {
                progress.spontaneousCount++;
            }
            if (report.getCompleted() == CompletionStatus.DONE) {
                progress.doneCount++;
            } else if (report.getCompleted() == CompletionStatus.PARTIAL) {
                progress.partialCount++;
            } else if (report.getCompleted() == CompletionStatus.NOT_DONE) {
                progress.notDoneCount++;
            }
        }

        return progressByWeek.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toDto()));
    }

    private static class MutableOfflineMissionProgress {
        private int assignedCount;
        private int submittedCount;
        private int doneCount;
        private int partialCount;
        private int notDoneCount;
        private int spontaneousCount;

        private DialogueProgressDto.OfflineMissionProgressDto toDto() {
            return new DialogueProgressDto.OfflineMissionProgressDto(
                    assignedCount,
                    submittedCount,
                    doneCount,
                    partialCount,
                    notDoneCount,
                    ratio(submittedCount, assignedCount),
                    ratio(doneCount + partialCount, assignedCount),
                    ratio(doneCount, submittedCount),
                    ratio(spontaneousCount, submittedCount)
            );
        }

        private double ratio(int numerator, int denominator) {
            if (denominator <= 0) {
                return 0.0;
            }
            return (double) numerator / denominator;
        }
    }
}
