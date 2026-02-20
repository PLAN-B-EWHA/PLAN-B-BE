package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.mission.MissionPhotoDTO;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.service.MissionPhotoService;
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
@Tag(name = "MissionPhoto", description = "미션 증빙 사진 API")
public class MissionPhotoController {

    private final MissionPhotoService missionPhotoService;

    @PostMapping("/missions/{missionId}/photos")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "미션 사진 업로드", description = "미션 증빙 사진을 업로드합니다.")
    public ResponseEntity<ApiResponse<MissionPhotoDTO>> uploadPhoto(
            @PathVariable UUID missionId,
            @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        MissionPhotoDTO photo = missionPhotoService.uploadPhoto(missionId, file, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("미션 사진이 업로드되었습니다.", photo));
    }

    @GetMapping("/missions/{missionId}/photos")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "미션 사진 목록", description = "미션 증빙 사진 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<MissionPhotoDTO>>> getPhotos(
            @PathVariable UUID missionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        List<MissionPhotoDTO> photos = missionPhotoService.getPhotos(missionId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(photos));
    }

    @DeleteMapping("/mission-photos/{photoId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "미션 사진 삭제", description = "미션 증빙 사진을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(
            @PathVariable UUID photoId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        missionPhotoService.deletePhoto(photoId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("미션 사진이 삭제되었습니다."));
    }

    @GetMapping("/mission-photos/{photoId}/download")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "미션 사진 다운로드", description = "미션 증빙 사진 파일을 다운로드합니다.")
    public ResponseEntity<Resource> downloadPhoto(
            @PathVariable UUID photoId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        try {
            Path filePath = missionPhotoService.getPhotoPath(photoId, currentUser.getUserId());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("파일을 읽을 수 없습니다.");
            }

            String fileName = resource.getFilename() != null ? resource.getFilename() : "mission-photo";
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            log.error("미션 사진 다운로드 실패 - photoId: {}", photoId, e);
            return ResponseEntity.badRequest().build();
        }
    }
}
