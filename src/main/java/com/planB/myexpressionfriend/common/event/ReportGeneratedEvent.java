package com.planB.myexpressionfriend.common.event;

import java.util.UUID;

public record ReportGeneratedEvent(
        UUID reportId,
        UUID userId
) {
}
