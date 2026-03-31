package myexpressionfriend_api.therapist.memo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.common.dto.common.PageResponseDTO;
import myexpressionfriend_api.common.util.SecurityContextUtil;
import myexpressionfriend_api.therapist.memo.domain.TherapistMemoStatus;
import myexpressionfriend_api.therapist.memo.dto.*;
import myexpressionfriend_api.therapist.memo.service.TherapistMemoLlmService;
import myexpressionfriend_api.therapist.memo.service.TherapistMemoService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "TherapistMemo", description = "치료사 메모 API")
public class TherapistMemoController {

    private final TherapistMemoService memoService;
    private final TherapistMemoLlmService memoLlmService;

    // ─── 치료사 엔드포인트 ─────────────────────────────────────────────

    @PostMapping("/api/therapist/patients/{childId}/memo")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "메모 임시저장", description = "세션 노트를 DRAFT 상태로 저장합니다. LLM 초안은 /regenerate 엔드포인트로 별도 요청하세요.")
    public ResponseEntity<ApiResponse<TherapistMemoResponseDTO>> createMemo(
            Authentication authentication,
            @PathVariable UUID childId,
            @Valid @RequestBody TherapistMemoCreateDTO dto
    ) {
        UUID therapistId = SecurityContextUtil.getCurrentUserId(authentication);
        TherapistMemoResponseDTO result = memoService.createMemo(therapistId, childId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("메모가 임시저장되었습니다.", result));
    }

    @PatchMapping("/api/therapist/patients/{childId}/memo/{memoId}")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "메모 수정", description = "DRAFT 상태의 메모만 수정 가능합니다.")
    public ResponseEntity<ApiResponse<TherapistMemoResponseDTO>> updateMemo(
            Authentication authentication,
            @PathVariable UUID childId,
            @PathVariable UUID memoId,
            @RequestBody TherapistMemoUpdateDTO dto
    ) {
        UUID therapistId = SecurityContextUtil.getCurrentUserId(authentication);
        TherapistMemoResponseDTO result = memoService.updateMemo(memoId, therapistId, dto);
        return ResponseEntity.ok(ApiResponse.success("메모가 수정되었습니다.", result));
    }

    @PatchMapping("/api/therapist/patients/{childId}/memo/{memoId}/publish")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "메모 발행", description = "DRAFT 메모를 발행합니다. isVisibleToParent=true이면 보호자에게 알림을 전송합니다.")
    public ResponseEntity<ApiResponse<TherapistMemoResponseDTO>> publishMemo(
            Authentication authentication,
            @PathVariable UUID childId,
            @PathVariable UUID memoId
    ) {
        UUID therapistId = SecurityContextUtil.getCurrentUserId(authentication);
        TherapistMemoResponseDTO result = memoService.publishMemo(memoId, therapistId);
        return ResponseEntity.ok(ApiResponse.success("메모가 발행되었습니다.", result));
    }

    @GetMapping("/api/therapist/patients/{childId}/memo")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "메모 목록 조회", description = "week_of, status 필터 적용 가능합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<TherapistMemoListItemDTO>>> getMemoList(
            Authentication authentication,
            @PathVariable UUID childId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekOf,
            @RequestParam(required = false) TherapistMemoStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID therapistId = SecurityContextUtil.getCurrentUserId(authentication);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                memoService.getMemoList(childId, therapistId, weekOf, status, pageable)));
    }

    @GetMapping("/api/therapist/patients/{childId}/memo/{memoId}")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "메모 상세 조회")
    public ResponseEntity<ApiResponse<TherapistMemoResponseDTO>> getMemoDetail(
            Authentication authentication,
            @PathVariable UUID childId,
            @PathVariable UUID memoId
    ) {
        UUID therapistId = SecurityContextUtil.getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(memoService.getMemoDetail(memoId, therapistId)));
    }

    @PostMapping("/api/therapist/patients/{childId}/memo/{memoId}/regenerate")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "LLM 초안 재생성", description = "치료사 피드백을 포함해 LLM 초안을 재생성합니다. (비동기)")
    public ResponseEntity<ApiResponse<Void>> regenerateDraft(
            Authentication authentication,
            @PathVariable UUID childId,
            @PathVariable UUID memoId,
            @RequestBody(required = false) TherapistMemoRegenerateDTO dto
    ) {
        UUID therapistId = SecurityContextUtil.getCurrentUserId(authentication);
        // 소유권 확인
        memoService.getMemoDetail(memoId, therapistId);

        String feedback = dto != null ? dto.getTherapistFeedback() : null;
        memoLlmService.regenerateDraft(memoId, feedback);
        return ResponseEntity.accepted().body(ApiResponse.success("LLM 초안 재생성 요청이 접수되었습니다."));
    }

    // ─── 보호자 엔드포인트 ─────────────────────────────────────────────

    @GetMapping("/api/parent/child/therapist-memo")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "보호자용 치료사 메모 조회",
            description = "발행됨(status=PUBLISHED) + 보호자 공개 설정된 메모만 반환합니다. content 필드는 포함되지 않습니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<ParentMemoResponseDTO>>> getPublishedMemoForParent(
            Authentication authentication,
            @RequestParam UUID childId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID parentId = SecurityContextUtil.getCurrentUserId(authentication);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                memoService.getPublishedMemoForParent(childId, parentId, pageable)));
    }
}
