package myexpressionfriend_api.therapist.memo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.service.ChildQueryService;
import myexpressionfriend_api.common.dto.common.PageResponseDTO;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import myexpressionfriend_api.common.exception.InvalidRequestException;
import myexpressionfriend_api.notification.domain.NotificationType;
import myexpressionfriend_api.notification.service.NotificationService;
import myexpressionfriend_api.therapist.memo.domain.TherapistMemo;
import myexpressionfriend_api.therapist.memo.domain.TherapistMemoStatus;
import myexpressionfriend_api.therapist.memo.dto.*;
import myexpressionfriend_api.therapist.memo.repository.TherapistMemoRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TherapistMemoService {

    private final TherapistMemoRepository memoRepository;
    private final ChildQueryService childQueryService;
    private final NotificationService notificationService;

    // ─── 생성 ──────────────────────────────────────────────────────────

    @Transactional
    public TherapistMemoResponseDTO createMemo(UUID therapistId, UUID childId, TherapistMemoCreateDTO dto) {
        Child child = childQueryService.getChildOrThrow(childId);

        if (!child.canAccess(therapistId)) {
            throw new InvalidRequestException("해당 아동에 대한 접근 권한이 없습니다.");
        }

        TherapistMemo memo = TherapistMemo.builder()
                .child(child)
                .therapistId(therapistId)
                .weekOf(dto.getWeekOf())
                .content(dto.getContent())
                .isVisibleToParent(dto.isVisibleToParent())
                .build();

        TherapistMemo saved = memoRepository.save(memo);

        log.info("치료사 메모 임시저장 memoId={}, therapistId={}, childId={}", saved.getId(), therapistId, childId);
        return TherapistMemoResponseDTO.from(saved);
    }

    // ─── 수정 ──────────────────────────────────────────────────────────

    @Transactional
    public TherapistMemoResponseDTO updateMemo(UUID memoId, UUID therapistId, TherapistMemoUpdateDTO dto) {
        TherapistMemo memo = findOwnMemoOrThrow(memoId, therapistId);

        if (!memo.isDraft()) {
            throw new InvalidRequestException("발행된 메모는 수정할 수 없습니다. (status=DRAFT 만 수정 가능)");
        }

        memo.updateDraft(dto.getContent(), dto.getParentContent(), dto.getHomePracticeTip(), dto.getIsVisibleToParent());
        return TherapistMemoResponseDTO.from(memo);
    }

    // ─── 발행 ──────────────────────────────────────────────────────────

    @Transactional
    public TherapistMemoResponseDTO publishMemo(UUID memoId, UUID therapistId) {
        TherapistMemo memo = findOwnMemoOrThrow(memoId, therapistId);

        if (!memo.isDraft()) {
            throw new InvalidRequestException("이미 발행된 메모입니다.");
        }

        memo.publish();

        // 보호자 공개 설정된 경우 주보호자에게 알림
        if (Boolean.TRUE.equals(memo.getIsVisibleToParent())) {
            memo.getChild().getPrimaryParentId().ifPresent(parentId ->
                    notificationService.saveAndSend(
                            parentId,
                            NotificationType.REPORT_GENERATED,
                            "치료사 메모가 도착했습니다",
                            memo.getChild().getName() + " 아동에 대한 치료사 메모가 등록됐습니다.",
                            memoId
                    )
            );
        }

        log.info("치료사 메모 발행 memoId={}", memoId);
        return TherapistMemoResponseDTO.from(memo);
    }

    // ─── 조회 ──────────────────────────────────────────────────────────

    public PageResponseDTO<TherapistMemoListItemDTO> getMemoList(
            UUID childId, UUID therapistId,
            LocalDate weekOf, TherapistMemoStatus status,
            Pageable pageable
    ) {
        return PageResponseDTO.from(
                memoRepository.findWithFilters(childId, therapistId, status, weekOf, pageable),
                TherapistMemoListItemDTO::from
        );
    }

    public TherapistMemoResponseDTO getMemoDetail(UUID memoId, UUID therapistId) {
        return TherapistMemoResponseDTO.from(findOwnMemoOrThrow(memoId, therapistId));
    }

    public PageResponseDTO<ParentMemoResponseDTO> getPublishedMemoForParent(
            UUID childId, UUID parentId, Pageable pageable
    ) {
        Child child = childQueryService.getChildOrThrow(childId);

        if (!child.canAccess(parentId)) {
            throw new InvalidRequestException("해당 아동에 대한 접근 권한이 없습니다.");
        }

        return PageResponseDTO.from(
                memoRepository.findByChild_ChildIdAndIsVisibleToParentTrueAndStatusOrderByWeekOfDesc(
                        childId, TherapistMemoStatus.PUBLISHED, pageable),
                ParentMemoResponseDTO::from
        );
    }

    // ─── private ─────────────────────────────────────────────────────

    private TherapistMemo findOwnMemoOrThrow(UUID memoId, UUID therapistId) {
        return memoRepository.findByIdAndTherapistId(memoId, therapistId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "메모를 찾을 수 없거나 접근 권한이 없습니다. memoId=" + memoId));
    }
}
