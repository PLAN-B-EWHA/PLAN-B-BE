package myexpressionfriend_api.homework.event;

import myexpressionfriend_api.homework.domain.StrategyFocus;

import java.util.UUID;

/**
 * 보호자가 숙제 리포트를 제출했을 때 발행된다.
 * 리스너에서 해당 아동의 치료사(ASSIGN_MISSION 권한 보유자)에게 알림을 전송한다.
 */
public record HomeworkSubmittedEvent(
        UUID homeworkId,
        UUID childId,
        String childName,
        StrategyFocus strategyFocus
) {}
