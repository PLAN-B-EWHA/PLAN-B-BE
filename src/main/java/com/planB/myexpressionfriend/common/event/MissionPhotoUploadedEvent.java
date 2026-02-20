package com.planB.myexpressionfriend.common.event;

import java.util.UUID;

public record MissionPhotoUploadedEvent(
        UUID missionId,
        UUID photoId,
        UUID therapistUserId,
        UUID uploaderUserId
) {
}
