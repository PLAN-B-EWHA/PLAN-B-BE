package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.domain.mission.MissionStatus;
import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.mission.AssignedMissionCreateDTO;
import com.planB.myexpressionfriend.common.dto.mission.AssignedMissionDTO;
import com.planB.myexpressionfriend.common.dto.mission.AssignedMissionDetailDTO;
import com.planB.myexpressionfriend.common.dto.mission.MissionBatchReviewRequestDTO;
import com.planB.myexpressionfriend.common.dto.mission.MissionBatchReviewResultDTO;
import com.planB.myexpressionfriend.common.dto.mission.MissionReviewQueueItemDTO;
import com.planB.myexpressionfriend.common.dto.mission.MissionSearchDTO;
import com.planB.myexpressionfriend.common.dto.mission.MissionStatusUpdateDTO;
import com.planB.myexpressionfriend.common.dto.note.PageResponseDTO;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.service.AssignedMissionService;
import com.planB.myexpressionfriend.common.util.PageableUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "AssignedMission", description = "할당 미션 API")
public class AssignedMissionController {

    private final AssignedMissionService missionService;

    @PostMapping("/children/{childId}/missions")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "미션 할당", description = "치료사가 특정 아동에게 미션을 할당합니다.")
    public ResponseEntity<ApiResponse<AssignedMissionDTO>> assignMission(
            @PathVariable UUID childId,
            @Valid @RequestBody AssignedMissionCreateDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        if (!dto.getChildId().equals(childId)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("경로 childId와 본문 childId가 일치하지 않습니다.", "INVALID_PATH_PARAM"));
        }

        AssignedMissionDTO mission = missionService.assignMission(dto, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("미션이 할당되었습니다.", mission));
    }

    @GetMapping("/missions/{missionId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "미션 상세 조회", description = "권한이 있는 사용자가 특정 미션의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<AssignedMissionDetailDTO>> getMission(
            @PathVariable UUID missionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        AssignedMissionDetailDTO mission = missionService.getMission(missionId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(mission));
    }

    @GetMapping("/children/{childId}/missions")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "아동별 미션 목록 조회", description = "특정 아동에 할당된 미션 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<AssignedMissionDTO>>> getMissionsByChild(
            @PathVariable UUID childId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "assignedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        Pageable pageable = PageableUtil.createPageable(page, size, sortBy, sortDirection, Sort.Direction.DESC);
        PageResponseDTO<AssignedMissionDTO> missions =
                missionService.getMissionsByChild(childId, currentUser.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(missions));
    }

    @GetMapping("/children/{childId}/missions/search")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "미션 검색", description = "상태, 치료사, 기간 조건으로 미션을 검색합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<AssignedMissionDTO>>> searchMissions(
            @PathVariable UUID childId,
            @RequestParam(required = false) MissionStatus status,
            @RequestParam(required = false) UUID therapistId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "assignedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        MissionSearchDTO searchDTO = MissionSearchDTO.builder()
                .childId(childId)
                .status(status)
                .therapistId(therapistId)
                .startDate(startDate)
                .endDate(endDate)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponseDTO<AssignedMissionDTO> missions = missionService.searchMissions(searchDTO, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(missions));
    }

    @GetMapping("/children/{childId}/missions/overdue")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "기한 초과 미션 조회", description = "기한이 지난 미션 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<AssignedMissionDTO>>> getOverdueMissions(
            @PathVariable UUID childId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        Pageable pageable = PageableUtil.createPageable(page, size);
        PageResponseDTO<AssignedMissionDTO> missions =
                missionService.getOverdueMissions(childId, currentUser.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(missions));
    }

    @GetMapping("/children/{childId}/missions/pending-verification")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "검토 대기 미션 조회", description = "검토가 필요한 완료 미션 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<AssignedMissionDetailDTO>>> getPendingVerificationMissions(
            @PathVariable UUID childId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        Pageable pageable = PageableUtil.createPageable(page, size);
        PageResponseDTO<AssignedMissionDetailDTO> missions =
                missionService.getPendingVerificationMissions(childId, currentUser.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(missions));
    }

    @GetMapping("/missions/review-queue")
    @PreAuthorize("hasAnyRole('THERAPIST', 'TEACHER')")
    @Operation(summary = "미션 검토 대기열 조회", description = "치료사 기준으로 검토 대기 중인 미션 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<MissionReviewQueueItemDTO>>> getReviewQueue(
            @RequestParam(required = false) UUID childId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        Pageable pageable = PageableUtil.createPageable(page, size, "completedAt", "DESC", Sort.Direction.DESC);
        PageResponseDTO<MissionReviewQueueItemDTO> queue =
                missionService.getReviewQueue(currentUser.getUserId(), childId, pageable);
        return ResponseEntity.ok(ApiResponse.success(queue));
    }

    @PatchMapping("/missions/review-queue/batch-verify")
    @PreAuthorize("hasAnyRole('THERAPIST', 'TEACHER')")
    @Operation(summary = "미션 일괄 검토", description = "여러 미션을 한 번에 승인 또는 반려 처리합니다.")
    public ResponseEntity<ApiResponse<MissionBatchReviewResultDTO>> batchVerifyMissions(
            @Valid @RequestBody MissionBatchReviewRequestDTO requestDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        MissionBatchReviewResultDTO result = missionService.batchReviewMissions(currentUser.getUserId(), requestDTO);
        return ResponseEntity.ok(ApiResponse.success("일괄 검토가 처리되었습니다.", result));
    }

    @PatchMapping("/missions/{missionId}/start")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "미션 시작", description = "부모가 미션을 시작 상태로 변경합니다.")
    public ResponseEntity<ApiResponse<AssignedMissionDTO>> startMission(
            @PathVariable UUID missionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        AssignedMissionDTO mission = missionService.startMission(missionId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("미션이 시작되었습니다.", mission));
    }

    @PatchMapping("/missions/{missionId}/complete")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "미션 완료", description = "부모가 미션 수행을 완료 처리합니다.")
    public ResponseEntity<ApiResponse<AssignedMissionDTO>> completeMission(
            @PathVariable UUID missionId,
            @Valid @RequestBody MissionStatusUpdateDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        AssignedMissionDTO mission = missionService.completeMission(missionId, dto, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("미션이 완료되었습니다.", mission));
    }

    @PatchMapping("/missions/{missionId}/verify")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "미션 검증", description = "치료사가 완료된 미션을 검증합니다.")
    public ResponseEntity<ApiResponse<AssignedMissionDTO>> verifyMission(
            @PathVariable UUID missionId,
            @Valid @RequestBody MissionStatusUpdateDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        AssignedMissionDTO mission = missionService.verifyMission(missionId, dto, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("미션이 검증되었습니다.", mission));
    }

    @PatchMapping("/missions/{missionId}/status")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "미션 상태 변경", description = "권한이 있는 사용자가 미션 상태를 변경합니다.")
    public ResponseEntity<ApiResponse<AssignedMissionDTO>> updateMissionStatus(
            @PathVariable UUID missionId,
            @Valid @RequestBody MissionStatusUpdateDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        if (dto.getStatus() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("status 값이 필요합니다.", "INVALID_STATUS"));
        }

        AssignedMissionDTO mission = missionService.updateMissionStatus(missionId, dto, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("미션 상태가 변경되었습니다.", mission));
    }

    @PatchMapping("/missions/{missionId}/cancel")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "미션 취소", description = "치료사가 미션을 취소합니다.")
    public ResponseEntity<ApiResponse<Void>> cancelMission(
            @PathVariable UUID missionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        missionService.cancelMission(missionId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("미션이 취소되었습니다."));
    }

    @DeleteMapping("/missions/{missionId}")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "미션 삭제", description = "치료사가 특정 미션을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteMission(
            @PathVariable UUID missionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        missionService.deleteMission(missionId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("미션이 삭제되었습니다."));
    }

    @GetMapping("/children/{childId}/missions/count")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "미션 개수 조회", description = "아동별 전체 또는 상태별 미션 개수를 조회합니다.")
    public ResponseEntity<ApiResponse<Long>> countMissions(
            @PathVariable UUID childId,
            @RequestParam(required = false) MissionStatus status,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        long count = status != null
                ? missionService.countMissionsByChildAndStatus(childId, currentUser.getUserId(), status)
                : missionService.countMissionsByChild(childId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}