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
@Tag(name = "MissionTemplate", description = "미션 템플릿 API")
public class MissionTemplateController {

    private final MissionTemplateService templateService;

    @PostMapping
    @PreAuthorize("hasRole('THERAPIST')")
    public ResponseEntity<ApiResponse<MissionTemplateDTO>> createTemplate(
            @Valid @RequestBody MissionTemplateCreateDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        MissionTemplateDTO template = templateService.createTemplate(dto);
        return ResponseEntity.ok(ApiResponse.success("템플릿이 생성되었습니다.", template));
    }

    @GetMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    public ResponseEntity<ApiResponse<MissionTemplateDTO>> getTemplate(
            @PathVariable UUID templateId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        MissionTemplateDTO template = templateService.getTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success(template));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
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
    public ResponseEntity<ApiResponse<Void>> activateTemplate(
            @PathVariable UUID templateId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        templateService.activateTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success("템플릿이 활성화되었습니다."));
    }

    @PatchMapping("/{templateId}/deactivate")
    @PreAuthorize("hasRole('THERAPIST')")
    public ResponseEntity<ApiResponse<Void>> deactivateTemplate(
            @PathVariable UUID templateId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        templateService.deactivateTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success("템플릿이 비활성화되었습니다."));
    }

    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasRole('THERAPIST')")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable UUID templateId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        templateService.deleteTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success("템플릿이 삭제되었습니다."));
    }

    @GetMapping("/stats/count")
    @PreAuthorize("hasRole('THERAPIST')")
    public ResponseEntity<ApiResponse<Long>> countTemplates(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        long count = templateService.countActiveTemplates();
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/stats/count/category/{category}")
    @PreAuthorize("hasRole('THERAPIST')")
    public ResponseEntity<ApiResponse<Long>> countByCategory(
            @PathVariable MissionCategory category,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        long count = templateService.countTemplatesByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/stats/count/llm")
    @PreAuthorize("hasRole('THERAPIST')")
    public ResponseEntity<ApiResponse<Long>> countLLMGenerated(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        long count = templateService.countLLMGeneratedTemplates();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
