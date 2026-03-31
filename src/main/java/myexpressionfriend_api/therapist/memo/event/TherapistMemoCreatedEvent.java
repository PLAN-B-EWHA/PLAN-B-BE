package myexpressionfriend_api.therapist.memo.event;

import java.util.UUID;

/**
 * 치료사 메모 생성 완료 이벤트.
 * 트랜잭션 커밋 후 LLM 초안 생성을 트리거합니다.
 */
public record TherapistMemoCreatedEvent(UUID memoId) {}
