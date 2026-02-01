package com.planB.myexpressionfriend.common.domain.note;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 노트 첨부파일 엔티티
 * - DB 저장: "notes/{childId}/{noteId}/{uuid}.jpg" (상대 경로)
 * - 실제 경로: {storage.base-path}/notes/{childId}/{noteId}/{uuid}.jpg
 *
 * 보안:
 * - 파일명 UUID 난독화
 * - 아동/노트별 폴더 정리
 * - Directory Traversal 방어
 */
@Entity
@Table(name = "note_assets", indexes = {
        @Index(name = "idx_assets_note", columnList = "note_id"),
        @Index(name = "idx_assets_type", columnList = "asset_type")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"note"})
@EntityListeners(AuditingEntityListener.class)
public class NoteAsset {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "asset_id", updatable = false, nullable = false)
    private UUID assetId;

    /**
     * 소속 노트
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    @Setter
    private ChildNote note;

    /**
     * 파일 저장 경로 (상대 경로)
     */
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    /**
     * 파일 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType type;

    /**
     * 원본 파일명 (사용자가 업로드한 파일명)
     */
    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    /**
     * 저장된 파일명 (UUID 기반 난독화)
     */
    @Column(name = "stored_file_name", nullable = false, length = 255)
    private String storedFileName;

    /**
     * 파일 크기 (bytes)
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * MIME 타입
     */
    @Column(name = "content_type", length = 100)
    private String contentType;

    /**
     * 썸네일 경로 (이미지인 경우)
     */
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;


    // ============= 정적 팩토리 메서드 =============

    /**
     * 파일 업로드 정보로 NoteAsset 생성
     *
     * @param originalFileName 원본 파일명
     * @param fileSize 파일 크기
     * @param contentType MIME 타입
     * @param childId 아동 ID
     * @param noteId 노트 ID
     * @return NoteAsset
     */
    public static NoteAsset createAsset(
            String originalFileName,
            Long fileSize,
            String contentType,
            UUID childId,
            UUID noteId
    ) {
        // 파일 타입 결정
        AssetType assetType = AssetType.fromFileName(originalFileName);

        // 파일 크기 검증
        assetType.validateFileSize(fileSize);

        // 파일 확장자 검증
        assetType.validateExtension(originalFileName);

        // 저장될 파일명 생성 (UUID 기반)
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String storedFileName = UUID.randomUUID() + extension;

        // 상대 경로 생성 (보안: 아동/노트별 격리)
        String fileUrl = String.format("notes/%s/%s/%s", childId, noteId, storedFileName);

        return NoteAsset.builder()
                .fileUrl(fileUrl)
                .type(assetType)
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .fileSize(fileSize)
                .contentType(contentType)
                .build();
    }

    // ============= 비즈니스 메서드 =============

    /**
     * 썸네일 경로 설정
     */
    public void setThumbnailUrl(String thumbnailUrl) {
        if (!type.needsThumbnail()) {
            throw new IllegalStateException("이미지 파일만 썸네일을 가질 수 있습니다");
        }
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * 이미지 파일인지 확인
     */
    public boolean isImage() {
        return this.type == AssetType.IMAGE;
    }

    /**
     * 비디오 파일인지 확인
     */
    public boolean isVideo() {
        return this.type == AssetType.VIDEO;
    }

    /**
     * 문서 파일인지 확인
     */
    public boolean isDocument() {
        return this.type == AssetType.DOCUMENT;
    }

    /**
     * 파일 크기를 MB 단위로 반환
     */
    public double getFileSizeInMB() {
        return fileSize / (1024.0 * 1024.0);
    }
}
