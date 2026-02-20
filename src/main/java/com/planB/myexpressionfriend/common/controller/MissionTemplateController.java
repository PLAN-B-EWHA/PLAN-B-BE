package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.domain.mission.MissionCategory;
import com.planB.myexpressionfriend.common.domain.mission.MissionDifficulty;
import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.mission.MissionTemplateCreateDTO;
import com.planB.myexpressionfriend.common.dto.mission.MissionTemplateDTO;
import com.planB.myexpressionfriend.common.dto.mission.MissionTemplateSearchDTO;
import com.planB.myexpressionfriend.common.dto.mission.MissionTemplateUpdateDTO;
import com.planB.myexpressionfriend.common.dto.note.PageResponseDTO;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.service.MissionTemplateService;
import com.planB.myexpressionfriend.common.util.PageableUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/mission-templates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "MissionTemplate", description = "미션 템플릿 API")
public class MissionTemplateController {

    private final MissionTemplateService templateService;

    @PostMapping
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "템플릿 생성", description = "미션 템플릿을 생성합니다. 치료사만 가능합니다.")
    public ResponseEntity<ApiResponse<MissionTemplateDTO>> createTemplate(
            @Valid @RequestBody MissionTemplateCreateDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("POST /api/mission-templates - userId: {}", currentUser.getUserId());
        MissionTemplateDTO template = templateService.createTemplate(dto);
        return ResponseEntity.ok(ApiResponse.success("템플릿이 생성되었습니다.", template));
    }

    @GetMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "템플릿 상세 조회", description = "특정 템플릿의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<MissionTemplateDTO>> getTemplate(
            @PathVariable UUID templateId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("GET /api/mission-templates/{} - userId: {}", templateId, currentUser.getUserId());
        MissionTemplateDTO template = templateService.getTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success(template));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "템플릿 목록", description = "활성 템플릿 목록을 페이지네이션하여 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<MissionTemplateDTO>>> getAllTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        Pageable pageable = PageableUtil.createPageable(page, size, sortBy, sortDirection, Sort.Direction.DESC);
        PageResponseDTO<MissionTemplateDTO> templates = templateService.getAllTemplates(pageable);
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "카테고리별 템플릿", description = "카테고리별 템플릿을 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<MissionTemplateDTO>>> getTemplatesByCategory(
            @PathVariable MissionCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        Pageable pageable = PageableUtil.createPageable(page, size, sortBy, sortDirection, Sort.Direction.DESC);
        PageResponseDTO<MissionTemplateDTO> templates = templateService.getTemplatesByCategory(category, pageable);
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    @GetMapping("/difficulty/{difficulty}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "난이도별 템플릿", description = "난이도별 템플릿을 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<MissionTemplateDTO>>> getTemplatesByDifficulty(
            @PathVariable MissionDifficulty difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        Pageable pageable = PageableUtil.createPageable(page, size, sortBy, sortDirection, Sort.Direction.DESC);
        PageResponseDTO<MissionTemplateDTO> templates = templateService.getTemplatesByDifficulty(difficulty, pageable);
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "템플릿 검색", description = "조건 기반으로 템플릿을 검색합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<MissionTemplateDTO>>> searchTemplates(
            @RequestParam(required = false) MissionCategory category,
            @RequestParam(required = false) MissionDifficulty difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean llmGenerated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        MissionTemplateSearchDTO searchDTO = MissionTemplateSearchDTO.builder()
                .category(category)
                .difficulty(difficulty)
                .keyword(keyword)
                .llmGenerated(llmGenerated)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponseDTO<MissionTemplateDTO> templates = templateService.searchTemplates(searchDTO);
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "템플릿 수정", description = "템플릿을 수정합니다. 치료사만 가능합니다.")
    public ResponseEntity<ApiResponse<MissionTemplateDTO>> updateTemplate(
            @PathVariable UUID templateId,
            @Valid @RequestBody MissionTemplateUpdateDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        MissionTemplateDTO template = templateService.updateTemplate(templateId, dto);
        return ResponseEntity.ok(ApiResponse.success("템플릿이 수정되었습니다.", template));
    }

    @PatchMapping("/{templateId}/activate")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "템플릿 활성화", description = "템플릿을 활성화합니다.")
    public ResponseEntity<ApiResponse<Void>> activateTemplate(
            @PathVariable UUID templateId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        templateService.activateTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success("템플릿이 활성화되었습니다."));
    }

    @PatchMapping("/{templateId}/deactivate")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "템플릿 비활성화", description = "템플릿을 비활성화합니다.")
    public ResponseEntity<ApiResponse<Void>> deactivateTemplate(
            @PathVariable UUID templateId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        templateService.deactivateTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success("템플릿이 비활성화되었습니다."));
    }

    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "템플릿 삭제", description = "템플릿을 삭제합니다. 치료사만 가능합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable UUID templateId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        templateService.deleteTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success("템플릿이 삭제되었습니다."));
    }

    @GetMapping("/stats/count")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "활성 템플릿 수", description = "활성 템플릿 총 개수를 조회합니다.")
    public ResponseEntity<ApiResponse<Long>> countTemplates(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        long count = templateService.countActiveTemplates();
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/stats/count/category/{category}")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "카테고리별 템플릿 수", description = "카테고리별 템플릿 개수를 조회합니다.")
    public ResponseEntity<ApiResponse<Long>> countByCategory(
            @PathVariable MissionCategory category,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        long count = templateService.countTemplatesByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/stats/count/llm")
    @PreAuthorize("hasRole('THERAPIST')")
    @Operation(summary = "LLM 템플릿 수", description = "LLM 생성 템플릿 개수를 조회합니다.")
    public ResponseEntity<ApiResponse<Long>> countLLMGenerated(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        long count = templateService.countLLMGeneratedTemplates();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
