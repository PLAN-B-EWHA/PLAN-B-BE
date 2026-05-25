package myexpressionfriend_api.statistics.dashboard.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.domain.ChildPermissionType;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.common.exception.AuthenticationFailedException;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import myexpressionfriend_api.statistics.dashboard.dto.DialogueProgressDto;
import myexpressionfriend_api.statistics.dialogue.dto.DialogueSummaryDto;
import myexpressionfriend_api.statistics.expression.dto.ExpressionSummaryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TherapistDashboardService {

    private final ChildRepository childRepository;
    private final DashboardSummaryAssembler dashboardSummaryAssembler;

    @Transactional(readOnly = true)
    public ExpressionSummaryDto getExpressionSummary(UUID userId, UUID childId) {
        loadChildWithPermissionCheck(userId, childId);
        return dashboardSummaryAssembler.buildExpressionSummary(childId, false);
    }

    @Transactional(readOnly = true)
    public List<DialogueSummaryDto> getAllDialogueSummaries(UUID userId, UUID childId) {
        loadChildWithPermissionCheck(userId, childId);
        return dashboardSummaryAssembler.buildDialogueSummaries(childId);
    }

    @Transactional(readOnly = true)
    public DialogueProgressDto getDialogueProgress(UUID userId, UUID childId) {
        loadChildWithPermissionCheck(userId, childId);
        return dashboardSummaryAssembler.buildDialogueProgress(childId, true);
    }

    public Child loadChildWithPermissionCheck(UUID userId, UUID childId) {
        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동 정보를 찾을 수 없습니다."));
        if (!child.hasPermission(userId, ChildPermissionType.VIEW_REPORT)) {
            throw new AuthenticationFailedException("리포트 조회 권한이 없습니다.");
        }
        return child;
    }
}
