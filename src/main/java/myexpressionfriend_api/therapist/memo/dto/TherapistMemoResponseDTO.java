package myexpressionfriend_api.therapist.memo.dto;

import myexpressionfriend_api.therapist.memo.domain.TherapistMemo;
import myexpressionfriend_api.therapist.memo.domain.TherapistMemoSource;
import myexpressionfriend_api.therapist.memo.domain.TherapistMemoStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TherapistMemoResponseDTO(
        UUID id,
        UUID childId,
        UUID therapistId,
        LocalDate weekOf,
        String content,
        String parentContent,
        String homePracticeTip,
        Boolean isVisibleToParent,
        TherapistMemoSource source,
        TherapistMemoStatus status,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TherapistMemoResponseDTO from(TherapistMemo memo) {
        return new TherapistMemoResponseDTO(
                memo.getId(),
                memo.getChild().getChildId(),
                memo.getTherapistId(),
                memo.getWeekOf(),
                memo.getContent(),
                memo.getParentContent(),
                memo.getHomePracticeTip(),
                memo.getIsVisibleToParent(),
                memo.getSource(),
                memo.getStatus(),
                memo.getPublishedAt(),
                memo.getCreatedAt(),
                memo.getUpdatedAt()
        );
    }
}
