package myexpressionfriend_api.therapist.memo.dto;

import myexpressionfriend_api.therapist.memo.domain.TherapistMemo;
import myexpressionfriend_api.therapist.memo.domain.TherapistMemoSource;
import myexpressionfriend_api.therapist.memo.domain.TherapistMemoStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TherapistMemoListItemDTO(
        UUID id,
        UUID childId,
        LocalDate weekOf,
        String contentPreview,
        TherapistMemoSource source,
        TherapistMemoStatus status,
        Boolean isVisibleToParent,
        LocalDateTime publishedAt,
        LocalDateTime createdAt
) {
    private static final int PREVIEW_LENGTH = 100;

    public static TherapistMemoListItemDTO from(TherapistMemo memo) {
        String preview = memo.getContent() != null && memo.getContent().length() > PREVIEW_LENGTH
                ? memo.getContent().substring(0, PREVIEW_LENGTH) + "..."
                : memo.getContent();

        return new TherapistMemoListItemDTO(
                memo.getId(),
                memo.getChild().getChildId(),
                memo.getWeekOf(),
                preview,
                memo.getSource(),
                memo.getStatus(),
                memo.getIsVisibleToParent(),
                memo.getPublishedAt(),
                memo.getCreatedAt()
        );
    }
}
