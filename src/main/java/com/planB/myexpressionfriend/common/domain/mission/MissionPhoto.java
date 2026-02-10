package com.planB.myexpressionfriend.common.domain.mission;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 미션 완료 증빙 사진 엔티티
 *
 * - DB 저장: "missions/{childId}/{missionId}/{uuid}.jpg" (상대 경로)
 * - 실제 경로: {storage.base-path}/missions/{childId}/{missionId}/{uuid}.jpg
 *
 * 보안:
 * - 파일명 UUID 난독화
 * - 아동/미션별 폴더 정리
 * - Directory Traversal 방어
 */
@Entity
@Table(name = "mission_photos", indexes = {
        @Index(name = "idx_mission_photos_mission", columnList = "mission_id")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"mission"})
@EntityListeners(AuditingEntityListener.class)
public class MissionPhoto {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "photo_id", updatable = false, nullable = false)
    private UUID photoId;

    /**
     * 소속 미션
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    @Setter
    private AssignedMission mission;

    /**
     * 파일 저장 경로 (상대 경로)
     */
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

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
     * 썸네일 경로
     */
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ============= 정적 팩토리 메서드 =============

    /**
     * 파일 업로드 정보로 MissionPhoto 생성
     *
     * @param originalFileName 원본 파일명
     * @param fileSize 파일 크기
     * @param contentType MIME 타입
     * @param childId 아동 ID
     * @param missionId 미션 ID
     * @return MissionPhoto
     */
    public static MissionPhoto createPhoto(
            String originalFileName,
            Long fileSize,
            String contentType,
            UUID childId,
            UUID missionId
    ) {
        // 파일 크기 검증 (10MB)
        if (fileSize > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다");
        }

        // 이미지 파일 검증
        if (!isImageFile(contentType)) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다");
        }

        // 저장될 파일명 생성 (UUID 기반)
        String extension = getFileExtension(originalFileName);
        String storedFileName = UUID.randomUUID() + extension;

        // 상대 경로 생성 (보안: 아동/미션별 격리)
        String fileUrl = String.format("missions/%s/%s/%s", childId, missionId, storedFileName);

        return MissionPhoto.builder()
                .fileUrl(fileUrl)
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
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * 파일 크기를 MB 단위로 반환
     */
    public double getFileSizeInMB() {
        return fileSize / (1024.0 * 1024.0);
    }

    // ============= 유틸리티 메서드 =============

    /**
     * 이미지 파일인지 확인
     */
    private static boolean isImageFile(String contentType) {
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith("image/");
    }

    /**
     * 파일 확장자 추출
     */
    private static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}