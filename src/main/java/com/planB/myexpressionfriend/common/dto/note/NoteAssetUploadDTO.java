package com.planB.myexpressionfriend.common.dto.note;

import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 파일 업로드 요청 DTO
 * MultipartFile을 직접 받기 위한 DTO
 */
@Getter
@Builder
public class NoteAssetUploadDTO {

    private UUID noteId;
    private MultipartFile file;

    /**
     * 파일 유효성 검증
     */
    public void validate() {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일은 필수입니다");
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().isEmpty()) {
            throw new IllegalArgumentException("파일명이 유효하지 않습니다");
        }

        // 파일 크기는 AssetType에서 검증됨
    }
}