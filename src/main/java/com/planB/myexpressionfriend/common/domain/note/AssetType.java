package com.planB.myexpressionfriend.common.domain.note;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * 노트 첨부파일 타입
 * - IMAGE: 이미지 파일 (jpg, png, gif, webp)
 * - VIDEO: 비디오 파일 (mp4, mov, avi)
 * - DOCUMENT: 문서 파일 (pdf)
 */
@Getter
@RequiredArgsConstructor
public enum AssetType {

    IMAGE("이미지", Arrays.asList("jpg", "jpeg", "png", "gif", "webp"), 5 * 1024 * 1024L), // 5MB
    VIDEO("비디오", Arrays.asList("mp4", "mov", "avi"), 10 * 1024 * 1024L), // 10MB
    DOCUMENT("문서", Arrays.asList("pdf"), 10 * 1024 * 1024L); // 10MB

    private final String displayName;
    private final List<String> allowedExtensions;
    private final Long maxSizeBytes;

    /**
     * 파일 확장자로 AssetType 결정
     *
     * @param fileName 파일명
     * @return AssetType
     * @throws IllegalArgumentException 지원하지 않는 파일 타입
     */
    public static AssetType fromFileName(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("올바른 파일명이 아닙니다");
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        return Arrays.stream(values())
                .filter(type -> type.allowedExtensions.contains(extension))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 파일 형식입니다: " + extension
                ));
    }

    /**
     * 파일 크기 검증
     *
     * @param fileSize 파일 크기 (bytes)
     * @throws IllegalArgumentException 파일 크기 초과
     */
    public void validateFileSize(Long fileSize) {
        if (fileSize == null || fileSize <= 0) {
            throw new IllegalArgumentException("파일 크기가 유효하지 않습니다");
        }

        if (fileSize > maxSizeBytes) {
            throw new IllegalArgumentException(
                    String.format("%s 파일은 최대 %dMB까지 업로드 가능합니다",
                            displayName, maxSizeBytes / (1024 * 1024))
            );
        }
    }

    /**
     * 확장자 검증
     *
     * @param fileName 파일명
     * @throws IllegalArgumentException 허용되지 않는 확장자
     */
    public void validateExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("올바른 파일명이 아닙니다");
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException(
                    String.format("%s 파일은 %s 형식만 허용됩니다",
                            displayName, String.join(", ", allowedExtensions))
            );
        }
    }

    /**
     * 썸네일 생성이 필요한 타입인지 확인
     */
    public boolean needsThumbnail() {
        return this == IMAGE;
    }
}
