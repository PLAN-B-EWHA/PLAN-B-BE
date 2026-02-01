package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.note.NoteType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ChildNote Repository
 *
 * 주요 기능:
 * - 권한 기반 노트 조회 (VIEW_REPORT 권한 필요)
 * - 페이징/필터링/정렬
 * - N+1 방지 (JOIN FETCH)
 */
@Repository
public interface ChildNoteRepository extends JpaRepository<ChildNote, UUID> {

    // ============= 기본 조회 (권한 검증 포함) =============

    /**
     * 노트 상세 조회 (권한 검증 포함, N+1 방지)
     *
     * @param noteId 노트 ID
     * @param userId 조회 요청 사용자 ID
     * @return Optional<ChildNote>
     */
    @Query("""
        SELECT n FROM ChildNote n
        JOIN FETCH n.child c
        JOIN FETCH n.author
        LEFT JOIN FETCH n.assets
        WHERE n.noteId = :noteId
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
    Optional<ChildNote> findByIdWithAuth(
            @Param("noteId") UUID noteId,
            @Param("userId") UUID userId
    );

    /**
     * 특정 아동의 노트 목록 조회 (권한 검증 포함, 페이징)
     *
     * @Param childId 아동 ID
     * @Param userId 조회 요청 사용자 ID
     * @Param pageable 페이징 정보
     * @return Page<ChildNote>
     */
    @Query("""
        SELECT n FROM ChildNote n
        JOIN FETCH n.author
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
    Page<ChildNote> findByChildIdWithAuth(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId,
            Pageable pageable
    );

    // ============= 필터링 조회 =============

    /**
     *
     * @Param childId 아동 ID
     * @Param userId 조회 요청 사용자 ID
     * @Param type 노트 타입
     * @Param pageable 페이징 정보
     * @return Page<ChildNote>
     */
    @Query("""
        SELECT n FROM ChildNote  n
        JOIN FETCH n.author
        WHERE n.child.childId = :childId
        AND n.type = :type
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
    Page<ChildNote> findByChildIdAndTypeWithAuth(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId,
            @Param("type") NoteType type,
            Pageable pageable
    );

    /**
     * 작성자별 조회 (권한 검증 포함, 페이징)
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @param authorId 작성자 ID
     * @param pageable 페이징 정보
     * @return Page<ChildNote>
     */
    @Query("""
        SELECT n FROM ChildNote n
        JOIN FETCH n.author
        WHERE n.child.childId = :childId
        AND n.author.userId = :authorId
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
    Page<ChildNote> findByChildIdAndAuthorWithAuth(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId,
            @Param("authorId") UUID authorId,
            Pageable pageable
    );

    /**
     * 날짜 범위별 조회 (권한 검증 포함, 페이징)
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @param startDate 시작일시
     * @param endDate 종료일시
     * @param pageable 페이징 정보
     * @return Page<ChildNote>
     */
    @Query("""
        SELECT n FROM ChildNote n
        JOIN FETCH n.author
        WHERE n.child.childId = :childId
        AND n.createdAt BETWEEN :startDate AND :endDate
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
    Page<ChildNote> findByChildIdAndDateRangeWithAuth(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // ============= 검색 =============

    /**
     * 키워드 검색 (제목 + 본문, 권한 검증 포함, 페이징)
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return Page<ChildNote>
     */
    @Query("""
        SELECT n FROM ChildNote n
        JOIN FETCH n.author
        WHERE n.child.childId = :childId
        AND (n.title LIKE %:keyword% OR n.content LIKE %:keyword%)
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
    Page<ChildNote> searchByKeywordWithAuth(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // ============= 통계 =============

    /**
     * 특정 아동의 노트 총 개수 (권한 검증 포함)
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @return 노트 개수
     */
    @Query("""
        SELECT COUNT(n) FROM ChildNote n
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
    long countByChildIdWithAuth(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId
    );

    /**
     * 노트 타입별 개수 (권한 검증 포함)
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @param type 노트 타입
     * @return 노트 개수
     */
    @Query("""
        SELECT COUNT(n) FROM ChildNote n
        WHERE n.child.childId = :childId
        AND n.type = :type
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
    long countByChildIdAndTypeWithAuth(
            @Param("childId") UUID childId,
            @Param("userId") UUID userId,
            @Param("type") NoteType type
    );

    // ============= 관리자용 (권한 검증 없음) =============

    /**
     * 특정 아동의 모든 노트 조회 (관리자용, 권한 검증 없음)
     *
     * @param childId 아동 ID
     * @return List<ChildNote>
     */
    @Query("""
        SELECT n FROM ChildNote n
        JOIN FETCH n.author
        WHERE n.child.childId = :childId
        ORDER BY n.createdAt DESC
        """)
    List<ChildNote> findAllByChildId(@Param("childId") UUID childId);

    /**
     * 특정 작성자의 모든 노트 조회 (관리자용, 권한 검증 없음)
     *
     * @param authorId 작성자 ID
     * @return List<ChildNote>
     */
    @Query("""
        SELECT n FROM ChildNote n
        JOIN FETCH n.child
        WHERE n.author.userId = :authorId
        ORDER BY n.createdAt DESC
        """)
    List<ChildNote> findAllByAuthorId(@Param("authorId") UUID authorId);
}
