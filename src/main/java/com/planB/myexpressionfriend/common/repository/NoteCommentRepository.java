package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.note.NoteComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * NoteComment Repository
 *
 * 주요 기능:
 * - 댓글/대댓글 조회
 * - 권한 검증 포함 조회
 * - 계층 구조 지원
 */
@Repository
public interface NoteCommentRepository extends JpaRepository<NoteComment, UUID> {

    // ============= 기본 조회 (권한 검증 포함) =============

    /**
     * 댓글 상세 조회 (권한 검증 포함)
     *
     * @param commentId 댓글 ID
     * @param userId 조회 요청 사용자 ID
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
     * 특정 노트의 모든 댓글 조회 (권한 검증 포함)
     * 최상위 댓글 + 대댓글 모두 포함
     *
     * @param noteId 노트 ID
     * @param userId 조회 요청 사용자 ID
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
     * 특정 노트의 최상위 댓글만 조회 (권한 검증 포함, 페이징)
     *
     * @param noteId 노트 ID
     * @param userId 조회 요청 사용자 ID
     * @param pageable 페이징 정보
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
     * 특정 댓글의 대댓글 목록 조회 (권한 검증 포함)
     *
     * @param parentCommentId 부모 댓글 ID
     * @param userId 조회 요청 사용자 ID
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

    // ============= 통계 =============

    /**
     * 특정 노트의 댓글 총 개수 (대댓글 포함)
     *
     * @param noteId 노트 ID
     * @return 댓글 개수
     */
    @Query("""
        SELECT COUNT(c) FROM NoteComment c
        WHERE c.note.noteId = :noteId
        """)
    long countByNoteId(@Param("noteId") UUID noteId);

    /**
     * 특정 노트의 최상위 댓글 개수 (대댓글 제외)
     *
     * @param noteId 노트 ID
     * @return 최상위 댓글 개수
     */
    @Query("""
        SELECT COUNT(c) FROM NoteComment c
        WHERE c.note.noteId = :noteId
        AND c.parentComment IS NULL
        """)
    long countTopLevelByNoteId(@Param("noteId") UUID noteId);

    /**
     * 특정 댓글의 대댓글 개수
     *
     * @param parentCommentId 부모 댓글 ID
     * @return 대댓글 개수
     */
    @Query("""
        SELECT COUNT(c) FROM NoteComment c
        WHERE c.parentComment.commentId = :parentCommentId
        """)
    long countRepliesByParentId(@Param("parentCommentId") UUID parentCommentId);

    // ============= 관리자용 (권한 검증 없음) =============

    /**
     * 특정 작성자의 모든 댓글 조회 (관리자용, 권한 검증 없음)
     *
     * @param authorId 작성자 ID
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
     * 특정 노트의 모든 댓글 삭제 (관리자용)
     *
     * @param noteId 노트 ID
     */
    @Query("""
        DELETE FROM NoteComment c
        WHERE c.note.noteId = :noteId
        """)
    void deleteByNoteId(@Param("noteId") UUID noteId);
}