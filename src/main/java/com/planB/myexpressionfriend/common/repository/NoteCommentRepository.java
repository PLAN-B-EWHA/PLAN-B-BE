package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.note.NoteComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * NoteComment Repository
 *
 * μ£Όμ” κΈ°λ¥:
 * - ?“κ?/?€?“κ? μ΅°ν
 * - κ¶ν• κ²€μ¦??¬ν•¨ μ΅°ν
 * - κ³„μΈµ κµ¬μ΅° μ§€??
 */
@Repository
public interface NoteCommentRepository extends JpaRepository<NoteComment, UUID> {

    // ============= κΈ°λ³Έ μ΅°ν (κ¶ν• κ²€μ¦??¬ν•¨) =============

    /**
     * ?“κ? ?μ„Έ μ΅°ν (κ¶ν• κ²€μ¦??¬ν•¨)
     *
     * @param commentId ?“κ? ID
     * @param userId μ΅°ν ?”μ²­ ?¬μ©??ID
     * @return Optional<NoteComment>
     */
    @Query("""
        SELECT c FROM NoteComment c
        JOIN FETCH c.note n
        JOIN FETCH c.author
        JOIN FETCH n.child ch
        WHERE c.commentId = :commentId
        AND EXISTS (
            SELECT 1 FROM ChildrenAuthorizedUser au
            WHERE au.child.childId = ch.childId
            AND au.user.userId = :userId
            AND au.isActive = true
            AND (
                au.isPrimary = true
                OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
            )
        )
        """)
    Optional<NoteComment> findByIdWithAuth(
            @Param("commentId") UUID commentId,
            @Param("userId") UUID userId
    );

    /**
     * ?Ήμ • ?ΈνΈ??λª¨λ“  ?“κ? μ΅°ν (κ¶ν• κ²€μ¦??¬ν•¨)
     * μµμƒ???“κ? + ?€?“κ? λª¨λ‘ ?¬ν•¨
     *
     * @param noteId ?ΈνΈ ID
     * @param userId μ΅°ν ?”μ²­ ?¬μ©??ID
     * @return List<NoteComment>
     */
    @Query("""
        SELECT c FROM NoteComment c
        JOIN FETCH c.author
        LEFT JOIN FETCH c.replies
        WHERE c.note.noteId = :noteId
        AND EXISTS (
            SELECT 1 FROM ChildrenAuthorizedUser au
            JOIN au.child ch
            JOIN ch.authorizedUsers cau
            WHERE cau.child.childId = (SELECT n.child.childId FROM ChildNote n WHERE n.noteId = :noteId)
            AND au.user.userId = :userId
            AND au.isActive = true
            AND (
                au.isPrimary = true
                OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
            )
        )
        ORDER BY c.createdAt ASC
        """)
    List<NoteComment> findByNoteIdWithAuth(
            @Param("noteId") UUID noteId,
            @Param("userId") UUID userId
    );

    /**
     * ?Ήμ • ?ΈνΈ??μµμƒ???“κ?λ§?μ΅°ν (κ¶ν• κ²€μ¦??¬ν•¨, ?μ΄μ§?
     *
     * @param noteId ?ΈνΈ ID
     * @param userId μ΅°ν ?”μ²­ ?¬μ©??ID
     * @param pageable ?μ΄μ§??•λ³΄
     * @return Page<NoteComment>
     */
    @Query("""
        SELECT c FROM NoteComment c
        JOIN FETCH c.author
        WHERE c.note.noteId = :noteId
        AND c.parentComment IS NULL
        AND EXISTS (
            SELECT 1 FROM ChildrenAuthorizedUser au
            JOIN au.child ch
            JOIN ch.authorizedUsers cau
            WHERE cau.child.childId = (SELECT n.child.childId FROM ChildNote n WHERE n.noteId = :noteId)
            AND au.user.userId = :userId
            AND au.isActive = true
            AND (
                au.isPrimary = true
                OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
            )
        )
        """)
    Page<NoteComment> findTopLevelByNoteIdWithAuth(
            @Param("noteId") UUID noteId,
            @Param("userId") UUID userId,
            Pageable pageable
    );

    /**
     * ?Ήμ • ?“κ????€?“κ? λª©λ΅ μ΅°ν (κ¶ν• κ²€μ¦??¬ν•¨)
     *
     * @param parentCommentId λ¶€λª??“κ? ID
     * @param userId μ΅°ν ?”μ²­ ?¬μ©??ID
     * @return List<NoteComment>
     */
    @Query("""
        SELECT c FROM NoteComment c
        JOIN FETCH c.author
        WHERE c.parentComment.commentId = :parentCommentId
        AND EXISTS (
            SELECT 1 FROM ChildrenAuthorizedUser au
            JOIN au.child ch
            JOIN ch.authorizedUsers cau
            WHERE cau.child.childId = (
                SELECT n.child.childId FROM ChildNote n 
                WHERE n.noteId = (
                    SELECT pc.note.noteId FROM NoteComment pc WHERE pc.commentId = :parentCommentId
                )
            )
            AND au.user.userId = :userId
            AND au.isActive = true
            AND (
                au.isPrimary = true
                OR com.planB.myexpressionfriend.common.domain.child.ChildPermissionType.VIEW_REPORT MEMBER OF au.permissions
            )
        )
        ORDER BY c.createdAt ASC
        """)
    List<NoteComment> findRepliesByParentIdWithAuth(
            @Param("parentCommentId") UUID parentCommentId,
            @Param("userId") UUID userId
    );

    // ============= ?µκ³„ =============

    /**
     * ?Ήμ • ?ΈνΈ???“κ? μ΄?κ°μ (?€?“κ? ?¬ν•¨)
     *
     * @param noteId ?ΈνΈ ID
     * @return ?“κ? κ°μ
     */
    @Query("""
        SELECT COUNT(c) FROM NoteComment c
        WHERE c.note.noteId = :noteId
        """)
    long countByNoteId(@Param("noteId") UUID noteId);

    /**
     * ?Ήμ • ?ΈνΈ??μµμƒ???“κ? κ°μ (?€?“κ? ?μ™Έ)
     *
     * @param noteId ?ΈνΈ ID
     * @return μµμƒ???“κ? κ°μ
     */
    @Query("""
        SELECT COUNT(c) FROM NoteComment c
        WHERE c.note.noteId = :noteId
        AND c.parentComment IS NULL
        """)
    long countTopLevelByNoteId(@Param("noteId") UUID noteId);

    /**
     * ?Ήμ • ?“κ????€?“κ? κ°μ
     *
     * @param parentCommentId λ¶€λª??“κ? ID
     * @return ?€?“κ? κ°μ
     */
    @Query("""
        SELECT COUNT(c) FROM NoteComment c
        WHERE c.parentComment.commentId = :parentCommentId
        """)
    long countRepliesByParentId(@Param("parentCommentId") UUID parentCommentId);

    // ============= κ΄€λ¦¬μ??(κ¶ν• κ²€μ¦??†μ) =============

    /**
     * ?Ήμ • ?‘μ„±?μ λª¨λ“  ?“κ? μ΅°ν (κ΄€λ¦¬μ?? κ¶ν• κ²€μ¦??†μ)
     *
     * @param authorId ?‘μ„±??ID
     * @return List<NoteComment>
     */
    @Query("""
        SELECT c FROM NoteComment c
        JOIN FETCH c.note
        WHERE c.author.userId = :authorId
        ORDER BY c.createdAt DESC
        """)
    List<NoteComment> findAllByAuthorId(@Param("authorId") UUID authorId);

    /**
     * ?Ήμ • ?ΈνΈ??λª¨λ“  ?“κ? ??  (κ΄€λ¦¬μ??
     *
     * @param noteId ?ΈνΈ ID
     */
    @Modifying
    @Query("""
        DELETE FROM NoteComment c
        WHERE c.note.noteId = :noteId
        """)
    void deleteByNoteId(@Param("noteId") UUID noteId);
}

