package com.planB.myexpressionfriend.common.event;

import java.util.UUID;

public record MissionCompletedEvent(
        UUID missionId,
        UUID therapistUserId,
        UUID parentUserId
) {
}
