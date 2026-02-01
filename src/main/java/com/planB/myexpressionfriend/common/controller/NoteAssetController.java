package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.note.NoteAssetDTO;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.service.NoteAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * NoteAsset Controller
 *
 * 파일 업로드/다운로드/삭제 API
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "NoteAsset", description = "노트 첨부파일 API")
public class NoteAssetController {

    private final NoteAssetService assetService;

    // ============= 파일 업로드 =============

    @PostMapping("/notes/{noteId}/assets")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "파일 업로드", description = "노트에 파일을 첨부합니다. 노트 작성자만 업로드 가능합니다.")
    public ResponseEntity<ApiResponse<NoteAssetDTO>> uploadFile(
            @PathVariable UUID noteId,
            @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("POST /api/notes/{}/assets - userId: {}, fileName: {}",
                noteId, currentUser.getUserId(), file.getOriginalFilename());

        NoteAssetDTO asset = assetService.uploadFile(noteId, file, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("파일이 업로드되었습니다",asset));
    }

    // ============= 파일 조회 =============

    @GetMapping("/notes/{noteId}/assets")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "첨부파일 목록 조회", description = "노트의 모든 첨부파일을 조회합니다.")
    public ResponseEntity<ApiResponse<List<NoteAssetDTO>>> getAssetsByNote(
            @PathVariable UUID noteId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("GET /api/notes/{}/assets - userId: {}", noteId, currentUser.getUserId());

        List<NoteAssetDTO> assets = assetService.getAssetsByNote(noteId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(assets));
    }

    @GetMapping("/assets/{assetId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "첨부파일 상세 조회", description = "특정 첨부파일의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<NoteAssetDTO>> getAsset(
            @PathVariable UUID assetId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("GET /api/assets/{} - userId: {}", assetId, currentUser.getUserId());

        NoteAssetDTO asset = assetService.getAsset(assetId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(asset));
    }

    // ============= 파일 다운로드 =============

    @GetMapping("/assets/{assetId}/download")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "파일 다운로드", description = "첨부파일을 다운로드합니다.")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable UUID assetId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("GET /api/assets/{}/download - userId: {}", assetId, currentUser.getUserId());

        try {
            // 파일 경로 조회 (권한 검증 포함)
            Path filePath = assetService.getFilePath(assetId, currentUser.getUserId());

            // 파일 리소스 생성
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("파일을 읽을 수 없습니다");
            }

            // 파일 정보 조회
            NoteAssetDTO asset = assetService.getAsset(assetId, currentUser.getUserId());

            // Content-Type 설정
            String contentType = asset.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + asset.getOriginalFileName() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("파일 다운로드 실패 - assetId: {}", assetId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ============= 파일 삭제 =============

    @DeleteMapping("/assets/{assetId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "파일 삭제", description = "첨부파일을 삭제합니다. 노트 작성자 또는 주보호자만 삭제 가능합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteAsset(
            @PathVariable UUID assetId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("DELETE /api/assets/{} - userId: {}", assetId, currentUser.getUserId());

        assetService.deleteAsset(assetId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("파일이 삭제되었습니다"));
    }

    // ============= 통계 =============

    @GetMapping("/children/{childId}/assets/storage")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "스토리지 사용량 조회", description = "특정 아동의 총 파일 크기를 조회합니다.")
    public ResponseEntity<ApiResponse<Long>> getStorageUsage(
            @PathVariable UUID childId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("GET /api/children/{}/assets/storage - userId: {}", childId, currentUser.getUserId());

        long totalSize = assetService.getTotalFileSize(childId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(totalSize));
    }
}
