package myexpressionfriend_api.statistics.dashboard.dto;

import java.util.List;
import java.util.UUID;

/**
 * 3-2 이번 주 하이라이트 카드
 */
public record WeeklyHighlightDto(
        UUID childId,
        List<String> highlights,  // 하이라이트 메시지 목록 (최대 2~3개)
        String fallbackMessage    // 하이라이트 없을 때 참여 격려 메시지
) {}
