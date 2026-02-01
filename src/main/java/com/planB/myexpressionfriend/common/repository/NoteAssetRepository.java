package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.note.AssetType;
import com.planB.myexpressionfriend.common.domain.note.NoteAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * NoteAsset Repository
 *
 * 주요 기능:
 * - 노트별 첨부파일 조회
 * - 권한 검증 포함 조회
 * - 파일 타입별 필터링
 */
@Repository
public interface NoteAssetRepository extends JpaRepository<NoteAsset, UUID> {

    // ============= 기본 조회 =============

    /**
     * 특정 노트의 모든 첨부파일 조회
     *
     * @param noteId 노트 ID
     * @return List<NoteAsset>
     */
    @Query("""
        SELECT a FROM NoteAsset a
        WHERE a.note.noteId = :noteId
        ORDER BY a.createdAt ASC
        """)
    List<NoteAsset> findByNoteId(@Param("noteId") UUID noteId);

    /**
     * 첨부파일 상세 조회 (권한 검증 포함)
     *
     * @param assetId 첨부파일 ID
     * @param userId 조회 요청 사용자 ID
     * @return Optional<NoteAsset>
     */
    @Query("""
        SELECT a FROM NoteAsset a
        JOIN FETCH a.note n
        JOIN FETCH n.child c
        WHERE a.assetId = :assetId
        AND EXISTS (
            SELECT 1 FROM ChildrenAuthorizedUser au
            WHERE au.child.childId = c.childId
            AND au.user.userId = :userId
            AND au.isActive = true
            AND (
                au.isPrimary = true
                OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
            )
        )
        """)
    Optional<NoteAsset> findByIdWithAuth(
            @Param("assetId") UUID assetId,
            @Param("userId") UUID userId
    );

    // ============= 타입별 조회 =============

    /**
     * 특정 노트의 특정 타입 첨부파일만 조회
     *
     * @param noteId 노트 ID
     * @param type 파일 타입
     * @return List<NoteAsset>
     */
    @Query("""
        SELECT a FROM NoteAsset a
        WHERE a.note.noteId = :noteId
        AND a.type = :type
        ORDER BY a.createdAt ASC
        """)
    List<NoteAsset> findByNoteIdAndType(
            @Param("noteId") UUID noteId,
            @Param("type") AssetType type
    );

    /**
     * 특정 아동의 모든 이미지 파일 조회 (권한 검증 포함)
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @return List<NoteAsset>
     */
    @Query("""
        SELECT a FROM NoteAsset a
        JOIN a.note n
        WHERE n.child.childId = :childId
        AND a.type = 'IMAGE'
        AND EXISTS (
            SELECT 1 FROM ChildrenAuthorizedUser au
            WHERE au.child.childId = :childId
            AND au.user.userId = :userId
            AND au.isActive = true
            AND (
                au.isPrimary = true
                OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
            )
        )
        ORDER BY a.createdAt DESC
        """)
    List<NoteAsset> findImagesByChildIdWithAuth(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId
    );

    // ============= 통계 =============

    /**
     * 특정 노트의 첨부파일 개수
     *
     * @param noteId 노트 ID
     * @return 첨부파일 개수
     */
    @Query("""
        SELECT COUNT(a) FROM NoteAsset a
        WHERE a.note.noteId = :noteId
        """)
    long countByNoteId(@Param("noteId") UUID noteId);

    /**
     * 특정 노트의 총 파일 크기 (bytes)
     *
     * @param noteId 노트 ID
     * @return 총 파일 크기
     */
    @Query("""
        SELECT COALESCE(SUM(a.fileSize), 0) FROM NoteAsset a
        WHERE a.note.noteId = :noteId
        """)
    long sumFileSizeByNoteId(@Param("noteId") UUID noteId);

    /**
     * 특정 아동의 총 파일 크기 (권한 검증 포함)
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @return 총 파일 크기
     */
    @Query("""
        SELECT COALESCE(SUM(a.fileSize), 0) FROM NoteAsset a
        JOIN a.note n
        WHERE n.child.childId = :childId
        AND EXISTS (
            SELECT 1 FROM ChildrenAuthorizedUser au
            WHERE au.child.childId = :childId
            AND au.user.userId = :userId
            AND au.isActive = true
            AND (
                au.isPrimary = true
                OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
            )
        )
        """)
    long sumFileSizeByChildIdWithAuth(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId
    );

    // ============= 삭제 지원 =============

    /**
     * 특정 노트의 모든 첨부파일 삭제
     *
     * @param noteId 노트 ID
     */
    @Query("""
        DELETE FROM NoteAsset a
        WHERE a.note.noteId = :noteId
        """)
    void deleteByNoteId(@Param("noteId") UUID noteId);
}
