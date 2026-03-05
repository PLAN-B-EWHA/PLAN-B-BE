package com.planB.myexpressionfriend.common.event;

import java.util.UUID;

public record NoteAssetUploadedEvent(
        UUID childId,
        UUID noteId,
        UUID assetId,
        UUID actorUserId
) {
}

