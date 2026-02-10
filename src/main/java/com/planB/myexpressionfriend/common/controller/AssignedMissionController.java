package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.domain.mission.MissionStatus;
import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.mission.*;
import com.planB.myexpressionfriend.common.dto.note.PageResponseDTO;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.service.AssignedMissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AssignedMission Controller
 *
 * 미션 할당, 조회, 상태 변경 API
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AssignedMission", description = "할당된 미션 API")
public class AssignedMissionController {

    private final AssignedMissionService missionService;

    // ============= 미션 할당 =============

    @PostMapping("/children/{childId}/missions")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "미션 할당", description = "아동에게 미션을 할당합니다. 치료사만 가능합니다.")
    public ResponseEntity<ApiResponse<AssignedMissionDTO>> assignMission(
            @PathVariable UUID childId,
            @Valid @RequestBody AssignedMissionCreateDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("POST /api/children/{}/missions - userId: {}", childId, currentUser.getUserId());

        // childId 일치 검증
        if (!dto.getChildId().equals(childId)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            "경로의 childId와 요청 본문의 childId가 일치하지 않습니다",
                            "INVALID_PATH_PARAM"
                    ));
        }

        AssignedMissionDTO mission = missionService.assignMission(dto, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("미션이 할당되었습니다", mission));
    }

    // ============= 미션 조회 =============

    @GetMapping("/missions/{missionId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "미션 조회", description = "특정 미션의 상세 정보를 조회합니다. VIEW_REPORT 권한이 필요합니다.")
    public ResponseEntity<ApiResponse<AssignedMissionDetailDTO>> getMission(
            @PathVariable UUID missionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("GET /api/missions/{} - userId: {}", missionId, currentUser.getUserId());

        AssignedMissionDetailDTO mission = missionService.getMission(missionId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(mission));
    }

    @GetMapping("/children/{childId}/missions")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "미션 목록 조회", description = "특정 아동의 미션 목록을 페이징하여 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<AssignedMissionDTO>>> getMissionsByChild(
            @PathVariable UUID childId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "assignedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("GET /api/children/{}/missions - userId: {}, page: {}",
                childId, currentUser.getUserId(), page);

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        PageResponseDTO<AssignedMissionDTO> missions =
                missionService.getMissionsByChild(childId, currentUser.getUserId(), pageable);

        return ResponseEntity.ok(ApiResponse.success(missions));
    }

    // ============= 미션 검색 =============

    @GetMapping("/children/{childId}/missions/search")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "미션 검색", description = "상태, 치료사, 날짜 범위로 미션을 검색합니다.")
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
        log.info("GET /api/children/{}/missions/search - userId: {}, status: {}",
                childId, currentUser.getUserId(), status);

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

        PageResponseDTO<AssignedMissionDTO> missions =
                missionService.searchMissions(searchDTO, currentUser.getUserId());

        return ResponseEntity.ok(ApiResponse.success(missions));
    }

    @GetMapping("/children/{childId}/missions/overdue")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "마감일 지난 미션 조회", description = "마감일이 지난 미완료 미션을 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<AssignedMissionDTO>>> getOverdueMissions(
            @PathVariable UUID childId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("GET /api/children/{}/missions/overdue - userId: {}", childId, currentUser.getUserId());

        Pageable pageable = PageRequest.of(page, size);
        PageResponseDTO<AssignedMissionDTO> missions =
                missionService.getOverdueMissions(childId, currentUser.getUserId(), pageable);

        return ResponseEntity.ok(ApiResponse.success(missions));
    }

    @GetMapping("/children/{childId}/missions/pending-verification")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "완료 대기 미션 조회", description = "치료사 검증이 필요한 완료된 미션을 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<AssignedMissionDetailDTO>>> getPendingVerificationMissions(
            @PathVariable UUID childId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("GET /api/children/{}/missions/pending-verification - userId: {}",
                childId, currentUser.getUserId());

        Pageable pageable = PageRequest.of(page, size);
        PageResponseDTO<AssignedMissionDetailDTO> missions =
                missionService.getPendingVerificationMissions(childId, currentUser.getUserId(), pageable);

        return ResponseEntity.ok(ApiResponse.success(missions));
    }

    // ============= 미션 상태 변경 =============

    @PatchMapping("/missions/{missionId}/start")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "미션 시작", description = "미션을 시작합니다. 부모만 가능합니다.")
    public ResponseEntity<ApiResponse<AssignedMissionDTO>> startMission(
            @PathVariable UUID missionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("PATCH /api/missions/{}/start - userId: {}", missionId, currentUser.getUserId());

        AssignedMissionDTO mission = missionService.startMission(missionId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("미션이 시작되었습니다", mission));
    }

    @PatchMapping("/missions/{missionId}/complete")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "미션 완료", description = "미션을 완료합니다. 부모만 가능합니다.")
    public ResponseEntity<ApiResponse<AssignedMissionDTO>> completeMission(
            @PathVariable UUID missionId,
            @Valid @RequestBody MissionStatusUpdateDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("PATCH /api/missions/{}/complete - userId: {}", missionId, currentUser.getUserId());

        AssignedMissionDTO mission = missionService.completeMission(
                missionId, dto, currentUser.getUserId()
        );
        return ResponseEntity.ok(ApiResponse.success("미션이 완료되었습니다", mission));
    }

    @PatchMapping("/missions/{missionId}/verify")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "미션 검증", description = "완료된 미션을 검증합니다. 치료사만 가능합니다.")
    public ResponseEntity<ApiResponse<AssignedMissionDTO>> verifyMission(
            @PathVariable UUID missionId,
            @Valid @RequestBody MissionStatusUpdateDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("PATCH /api/missions/{}/verify - userId: {}", missionId, currentUser.getUserId());

        AssignedMissionDTO mission = missionService.verifyMission(
                missionId, dto, currentUser.getUserId()
        );
        return ResponseEntity.ok(ApiResponse.success("미션이 검증되었습니다", mission));
    }

    @PatchMapping("/missions/{missionId}/cancel")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "미션 취소", description = "미션을 취소합니다. 할당한 치료사만 가능합니다.")
    public ResponseEntity<ApiResponse<Void>> cancelMission(
            @PathVariable UUID missionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("PATCH /api/missions/{}/cancel - userId: {}", missionId, currentUser.getUserId());

        missionService.cancelMission(missionId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("미션이 취소되었습니다"));
    }

    // ============= 미션 삭제 =============

    @DeleteMapping("/missions/{missionId}")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "미션 삭제", description = "미션을 삭제합니다. 할당한 치료사만 가능합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteMission(
            @PathVariable UUID missionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("DELETE /api/missions/{} - userId: {}", missionId, currentUser.getUserId());

        missionService.deleteMission(missionId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("미션이 삭제되었습니다"));
    }

    // ============= 통계 =============

    @GetMapping("/children/{childId}/missions/count")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "미션 개수 조회", description = "특정 아동의 미션 개수를 조회합니다.")
    public ResponseEntity<ApiResponse<Long>> countMissions(
            @PathVariable UUID childId,
            @RequestParam(required = false) MissionStatus status,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("GET /api/children/{}/missions/count - userId: {}, status: {}",
                childId, currentUser.getUserId(), status);

        long count;
        if (status != null) {
            count = missionService.countMissionsByChildAndStatus(
                    childId, currentUser.getUserId(), status
            );
        } else {
            count = missionService.countMissionsByChild(childId, currentUser.getUserId());
        }

        return ResponseEntity.ok(ApiResponse.success(count));
    }
}