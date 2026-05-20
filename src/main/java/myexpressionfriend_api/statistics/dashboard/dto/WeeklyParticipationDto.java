package myexpressionfriend_api.statistics.dashboard.dto;

import java.util.List;
import java.util.UUID;

/**
 * 3-1 주간 참여 달성률 카드
 */
public record WeeklyParticipationDto(
        UUID childId,
        int completedDays,
        int gameCompletedDays,
        int offlineMissionCompletedDays,
        int recommendedPerWeek,
        boolean goalAchieved,
        String displayMessage,
        List<Boolean> dayMarkers  // 이번 주 요일별 학습 여부 (월~일, 7개)
) {}
