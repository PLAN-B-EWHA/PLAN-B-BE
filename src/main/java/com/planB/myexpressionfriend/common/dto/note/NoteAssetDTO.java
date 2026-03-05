package com.planB.myexpressionfriend.common.dto.note;

import com.planB.myexpressionfriend.common.domain.note.AssetType;
import com.planB.myexpressionfriend.common.domain.note.NoteAsset;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 */
@Getter
@Builder
public class NoteAssetDTO {

    private UUID assetId;
    private UUID noteId;
    private String fileUrl;
    private AssetType type;
    private String typeDisplayName;
    private String originalFileName;
    private Long fileSize;
    private String fileSizeReadable; // "1.5 MB"
    private String contentType;
    private String thumbnailUrl;
    private boolean isImage;
    private boolean isVideo;
    private boolean isDocument;
    private LocalDateTime createdAt;

    /**
     */
    public static NoteAssetDTO from(NoteAsset asset) {
        return NoteAssetDTO.builder()
                .assetId(asset.getAssetId())
                .noteId(asset.getNote().getNoteId())
                .fileUrl(asset.getFileUrl())
                .type(asset.getType())
                .typeDisplayName(asset.getType().getDisplayName())
                .originalFileName(asset.getOriginalFileName())
                .fileSize(asset.getFileSize())
                .fileSizeReadable(formatFileSize(asset.getFileSize()))
                .contentType(asset.getContentType())
                .thumbnailUrl(asset.getThumbnailUrl())
                .isImage(asset.isImage())
                .isVideo(asset.isVideo())
                .isDocument(asset.isDocument())
                .createdAt(asset.getCreatedAt())
                .build();
    }

    /**
     */
    private static String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) {
            return "0 B";
        }

        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double size = bytes.doubleValue();

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", size, units[unitIndex]);
    }
}