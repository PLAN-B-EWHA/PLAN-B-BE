package com.planB.myexpressionfriend.common.event;

import java.util.UUID;

public record NoteCommentCreatedEvent(
        UUID childId,
        UUID noteId,
        UUID commentId,
        UUID actorUserId,
        boolean reply
) {
}

