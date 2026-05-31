package myexpressionfriend_api.report.event;

import java.util.UUID;

public record ReportPublishedEvent(
        UUID reportId,
        UUID childId,
        String childName,
        UUID parentUserId
) {
}
