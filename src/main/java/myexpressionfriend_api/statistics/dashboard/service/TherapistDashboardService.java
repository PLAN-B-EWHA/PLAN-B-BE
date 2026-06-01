package myexpressionfriend_api.statistics.dashboard.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.child.service.ChildPermissionChecker;
import myexpressionfriend_api.statistics.dashboard.dto.DialogueProgressDto;
import myexpressionfriend_api.statistics.dashboard.service.ViewerRole;
import myexpressionfriend_api.statistics.dashboard.dto.WeeklyHighlightDto;
import myexpressionfriend_api.statistics.dashboard.dto.WeeklyParticipationDto;
import myexpressionfriend_api.statistics.dialogue.dto.DialogueSummaryDto;
import myexpressionfriend_api.statistics.expression.dto.ExpressionSummaryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TherapistDashboardService {

    private final ChildPermissionChecker childPermissionChecker;
    private final DashboardSummaryAssembler dashboardSummaryAssembler;

    @Transactional(readOnly = true)
    public ExpressionSummaryDto getExpressionSummary(UUID userId, UUID childId) {
        childPermissionChecker.checkAccess(userId, childId);
        return dashboardSummaryAssembler.buildExpressionSummary(childId, false);
    }

    @Transactional(readOnly = true)
    public List<DialogueSummaryDto> getAllDialogueSummaries(UUID userId, UUID childId) {
        childPermissionChecker.checkAccess(userId, childId);
        return dashboardSummaryAssembler.buildDialogueSummaries(childId);
    }

    @Transactional(readOnly = true)
    public WeeklyParticipationDto getWeeklyParticipation(UUID userId, UUID childId) {
        childPermissionChecker.checkAccess(userId, childId);
        return dashboardSummaryAssembler.buildWeeklyParticipation(childId);
    }

    @Transactional(readOnly = true)
    public WeeklyHighlightDto getWeeklyHighlight(UUID userId, UUID childId) {
        childPermissionChecker.checkAccess(userId, childId);
        return dashboardSummaryAssembler.buildWeeklyHighlight(childId);
    }

    @Transactional(readOnly = true)
    public DialogueProgressDto getDialogueProgress(UUID userId, UUID childId) {
        childPermissionChecker.checkAccess(userId, childId);
        return dashboardSummaryAssembler.buildDialogueProgress(childId, ViewerRole.THERAPIST);
    }
}
