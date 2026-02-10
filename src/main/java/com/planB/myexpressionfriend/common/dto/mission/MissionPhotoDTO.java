package com.planB.myexpressionfriend.common.dto.mission;

import com.planB.myexpressionfriend.common.domain.mission.MissionPhoto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 미션 사진 응답 DTO
 */
@Getter
@Builder
public class MissionPhotoDTO {

    private UUID photoId;
    private String fileUrl;
    private String originalFileName;
    private Long fileSize;
    private String contentType;
    private String thumbnailUrl;
    private LocalDateTime createdAt;

    /**
     * Entity → DTO 변환
     */
    public static MissionPhotoDTO from(MissionPhoto photo) {
        if (photo == null) {
            return null;
        }

        return MissionPhotoDTO.builder()
                .photoId(photo.getPhotoId())
                .fileUrl(photo.getFileUrl())
                .originalFileName(photo.getOriginalFileName())
                .fileSize(photo.getFileSize())
                .contentType(photo.getContentType())
                .thumbnailUrl(photo.getThumbnailUrl())
                .createdAt(photo.getCreatedAt())
                .build();
    }
}