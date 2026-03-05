package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.note.AssetType;
import com.planB.myexpressionfriend.common.domain.note.NoteAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * NoteAsset Repository
 *
 */
@Repository
public interface NoteAssetRepository extends JpaRepository<NoteAsset, UUID> {


    /**
     *
     * @param noteId ?명듃 ID
     * @return List<NoteAsset>
     */
    @Query("""
        SELECT a FROM NoteAsset a
        WHERE a.note.noteId = :noteId
        ORDER BY a.createdAt ASC
        """)
    List<NoteAsset> findByNoteId(@Param("noteId") UUID noteId);

    /**
     *
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


    /**
     *
     * @param noteId ?명듃 ID
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
     *
     * @param childId ?꾨룞 ID
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

    // ============= ?듦퀎 =============

    /**
     *
     * @param noteId ?명듃 ID
     */
    @Query("""
        SELECT COUNT(a) FROM NoteAsset a
        WHERE a.note.noteId = :noteId
        """)
    long countByNoteId(@Param("noteId") UUID noteId);

    /**
     *
     * @param noteId ?명듃 ID
     */
    @Query("""
        SELECT COALESCE(SUM(a.fileSize), 0) FROM NoteAsset a
        WHERE a.note.noteId = :noteId
        """)
    long sumFileSizeByNoteId(@Param("noteId") UUID noteId);

    /**
     *
     * @param childId ?꾨룞 ID
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


    /**
     *
     * @param noteId ?명듃 ID
     */
    @Modifying
    @Query("""
        DELETE FROM NoteAsset a
        WHERE a.note.noteId = :noteId
        """)
    void deleteByNoteId(@Param("noteId") UUID noteId);
}