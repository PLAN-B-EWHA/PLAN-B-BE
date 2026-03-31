package myexpressionfriend_api.therapist.memo.dto;

import myexpressionfriend_api.therapist.memo.domain.TherapistMemo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 보호자에게 공개되는 메모 응답 DTO.
 * 치료사 내부 노트(content)는 포함하지 않습니다.
 */
public record ParentMemoResponseDTO(
        UUID id,
        UUID childId,
        LocalDate weekOf,
        String parentContent,
        String homePracticeTip,
        LocalDateTime publishedAt
) {
    public static ParentMemoResponseDTO from(TherapistMemo memo) {
        return new ParentMemoResponseDTO(
                memo.getId(),
                memo.getChild().getChildId(),
                memo.getWeekOf(),
                memo.getParentContent(),
                memo.getHomePracticeTip(),
                memo.getPublishedAt()
        );
    }
}
