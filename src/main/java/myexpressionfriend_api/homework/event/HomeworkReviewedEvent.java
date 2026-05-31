package myexpressionfriend_api.homework.event;

import myexpressionfriend_api.homework.domain.StrategyFocus;

import java.util.UUID;

/**
 * 치료사가 숙제 검토를 완료했을 때 발행된다.
 * 리스너에서 숙제를 제출한 보호자에게 알림을 전송한다.
 */
public record HomeworkReviewedEvent(
        UUID homeworkId,
        UUID childId,
        String childName,
        StrategyFocus strategyFocus,
        UUID reportedByUserId
) {}
