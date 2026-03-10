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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "NoteAsset", description = "노트 첨부파일 API")
public class NoteAssetController {

    private final NoteAssetService assetService;

    @PostMapping("/notes/{noteId}/assets")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "첨부파일 업로드", description = "노트에 첨부파일을 업로드합니다.")
    public ResponseEntity<ApiResponse<NoteAssetDTO>> uploadFile(
            @PathVariable UUID noteId,
            @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        NoteAssetDTO asset = assetService.uploadFile(noteId, file, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("파일이 업로드되었습니다.", asset));
    }

    @GetMapping("/notes/{noteId}/assets")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "노트별 첨부파일 목록 조회", description = "특정 노트에 연결된 첨부파일 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<NoteAssetDTO>>> getAssetsByNote(
            @PathVariable UUID noteId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        List<NoteAssetDTO> assets = assetService.getAssetsByNote(noteId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(assets));
    }

    @GetMapping("/assets/{assetId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "첨부파일 상세 조회", description = "특정 첨부파일의 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<NoteAssetDTO>> getAsset(
            @PathVariable UUID assetId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        NoteAssetDTO asset = assetService.getAsset(assetId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(asset));
    }

    @GetMapping("/assets/{assetId}/download")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "첨부파일 다운로드", description = "특정 첨부파일을 다운로드합니다.")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable UUID assetId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        try {
            Path filePath = assetService.getFilePath(assetId, currentUser.getUserId());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("파일을 찾을 수 없습니다.");
            }

            NoteAssetDTO asset = assetService.getAsset(assetId, currentUser.getUserId());
            String contentType = asset.getContentType() == null ? "application/octet-stream" : asset.getContentType();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + asset.getOriginalFileName() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            log.error("파일 다운로드 실패 - assetId={}", assetId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/assets/{assetId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "첨부파일 삭제", description = "특정 첨부파일을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteAsset(
            @PathVariable UUID assetId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        assetService.deleteAsset(assetId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("파일이 삭제되었습니다."));
    }

    @GetMapping("/children/{childId}/assets/storage")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "첨부파일 저장 용량 조회", description = "특정 아동이 사용 중인 첨부파일 저장 용량을 조회합니다.")
    public ResponseEntity<ApiResponse<Long>> getStorageUsage(
            @PathVariable UUID childId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        long totalSize = assetService.getTotalFileSize(childId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(totalSize));
    }
}