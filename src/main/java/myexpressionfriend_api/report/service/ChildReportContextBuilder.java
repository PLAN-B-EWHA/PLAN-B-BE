package myexpressionfriend_api.report.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.homework.domain.HomeworkAssignment;
import myexpressionfriend_api.homework.domain.HomeworkReport;
import myexpressionfriend_api.homework.domain.HomeworkStatus;
import myexpressionfriend_api.homework.repository.HomeworkAssignmentRepository;
import myexpressionfriend_api.homework.repository.HomeworkReportRepository;
import myexpressionfriend_api.statistics.dialogue.domain.DialogueStatSummary;
import myexpressionfriend_api.statistics.dialogue.repository.DialogueStatSummaryRepository;
import myexpressionfriend_api.statistics.expression.domain.ExpressionStatSummary;
import myexpressionfriend_api.statistics.expression.repository.ExpressionStatSummaryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChildReportContextBuilder {

    private final DialogueStatSummaryRepository dialogueStatSummaryRepository;
    private final ExpressionStatSummaryRepository expressionStatSummaryRepository;
    private final HomeworkReportRepository homeworkReportRepository;
    private final HomeworkAssignmentRepository homeworkAssignmentRepository;

    @Transactional(readOnly = true)
    public String build(UUID childId) {
        StringBuilder sb = new StringBuilder();
        appendDialogueStats(sb, childId);
        appendExpressionStats(sb, childId);
        appendHomeworkStats(sb, childId);
        return sb.toString();
    }

    private void appendDialogueStats(StringBuilder sb, UUID childId) {
        List<DialogueStatSummary> stats = dialogueStatSummaryRepository.findByChild_ChildId(childId);
        if (stats.isEmpty()) return;

        sb.append("[대화 통계]\n");
        stats.stream()
                .sorted(Comparator.comparingInt(s -> s.getTheme().getWeekNumber()))
                .limit(6)
                .forEach(s -> sb
                        .append("- ").append(s.getTheme().getDisplayName())
                        .append(": EMA=").append(fmt(s.getEmaValue()))
                        .append(", 점수율=").append(fmt(s.getScoreRate()))
                        .append(", 추세=").append(dash(s.getTrendDirection()))
                        .append(", 신뢰도=").append(dash(s.getConfidenceLevel()))
                        .append(", 세션수=").append(s.getSessionCount())
                        .append('\n'));
    }

    private void appendExpressionStats(StringBuilder sb, UUID childId) {
        List<ExpressionStatSummary> stats = expressionStatSummaryRepository.findByChild_ChildId(childId);
        if (stats.isEmpty()) return;

        sb.append("[표정 인식 통계]\n");
        stats.stream()
                .sorted(Comparator.comparingDouble(ExpressionStatSummary::getSuccessRate))
                .limit(4)
                .forEach(s -> sb
                        .append("- ").append(s.getEmotionTarget())
                        .append(": 성공률=").append(fmt(s.getSuccessRate()))
                        .append(", 유창성=").append(fmt(s.getFluencyIndex()))
                        .append(", 추세=").append(dash(s.getTrendDirection()))
                        .append(", 신뢰도=").append(dash(s.getConfidenceLevel()))
                        .append('\n'));
    }

    private void appendHomeworkStats(StringBuilder sb, UUID childId) {
        List<HomeworkAssignment> assignments = homeworkAssignmentRepository.findByChild_ChildId(childId);
        long total = assignments.size();
        long submitted = assignments.stream()
                .filter(a -> a.getStatus() == HomeworkStatus.SUBMITTED || a.getStatus() == HomeworkStatus.REVIEWED)
                .count();

        sb.append("[오프라인 미션]\n")
                .append("- 전체=").append(total)
                .append(", 제출=").append(submitted)
                .append(", 제출률=").append(total > 0 ? fmt((double) submitted / total) : "-")
                .append('\n');

        List<HomeworkReport> reports = homeworkReportRepository.findByHomework_Child_ChildId(childId);
        String recentObservations = reports.stream()
                .sorted(Comparator.comparing(HomeworkReport::getReportedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .map(r -> "  · " + dash(r.getParentObservation()))
                .reduce("", (a, b) -> a + b + "\n");

        if (!recentObservations.isBlank()) {
            sb.append("[최근 보호자 관찰 (최대 3건)]\n").append(recentObservations);
        }
    }

    private String fmt(Double value) {
        return value == null ? "-" : String.format("%.2f", value);
    }

    private String dash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
