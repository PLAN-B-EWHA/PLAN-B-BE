package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.note.NoteComment;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.dto.note.*;
import com.planB.myexpressionfriend.common.repository.ChildNoteRepository;
import com.planB.myexpressionfriend.common.repository.NoteCommentRepository;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * NoteComment Service
 *
 * 책임:
 * - 댓글/대댓글 CRUD
 * - 권한 검증 (VIEW_REPORT)
 * - 계층 구조 관리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NoteCommentService {

    private final NoteCommentRepository commentRepository;
    private final ChildNoteRepository noteRepository;
    private final UserRepository userRepository;

    // ============= 댓글 생성 =============

    /**
     * 댓글 생성 (최상위 댓글 또는 대댓글)
     * 권한: VIEW_REPORT 필요 (노트 조회 가능하면 댓글 작성 가능)
     *
     * @param dto 생성 요청 DTO
     * @param authorId 작성자 ID
     * @return 생성된 댓글 DTO
     */
    @Transactional
    public NoteCommentDTO createComment(NoteCommentCreateDTO dto, UUID authorId) {
        log.info("댓글 생성 시작 - noteId: {}, authorId: {}, isReply: {}",
                dto.getNoteId(), authorId, dto.getParentCommentId() != null);

        // 1. 노트 조회 (권한 검증 포함)
        ChildNote note = noteRepository.findByIdWithAuth(dto.getNoteId(), authorId)
                .orElseThrow(() -> new AccessDeniedException("노트 조회 권한이 없거나 존재하지 않는 노트입니다"));

        // 2. 작성자 조회
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));

        // 3. 부모 댓글 확인 (대댓글인 경우)
        NoteComment parentComment = null;
        if (dto.getParentCommentId() != null) {
            parentComment = commentRepository.findByIdWithAuth(dto.getParentCommentId(), authorId)
                    .orElseThrow(() -> new AccessDeniedException("부모 댓글 조회 권한이 없거나 존재하지 않는 댓글입니다"));

            // 대댓글의 대댓글은 불가 (1depth만 허용)
            if (!parentComment.isTopLevel()) {
                throw new IllegalArgumentException("대댓글에는 답글을 달 수 없습니다");
            }
        }

        // 4. 댓글 생성
        NoteComment comment = NoteComment.builder()
                .note(note)
                .author(author)
                .parentComment(parentComment)
                .content(dto.getContent())
                .isDeleted(false)
                .build();

        // 5. 연관관계 설정
        note.addComment(comment);
        if (parentComment != null) {
            parentComment.addReply(comment);
        }

        // 6. 저장
        NoteComment savedComment = commentRepository.save(comment);
        log.info("댓글 생성 완료 - commentId: {}", savedComment.getCommentId());

        return NoteCommentDTO.from(savedComment);
    }

    // ============= 댓글 조회 =============

    /**
     * 노트의 모든 댓글 조회 (계층 구조)
     * 권한: VIEW_REPORT 필요
     *
     * @param noteId 노트 ID
     * @param userId 조회 요청 사용자 ID
     * @return 댓글 목록 (최상위 + 대댓글)
     */
    public List<NoteCommentDTO> getCommentsByNote(UUID noteId, UUID userId) {
        log.debug("노트 댓글 조회 - noteId: {}, userId: {}", noteId, userId);

        List<NoteComment> comments = commentRepository.findByNoteIdWithAuth(noteId, userId);

        // 최상위 댓글만 반환 (대댓글은 replies에 포함됨)
        return comments.stream()
                .filter(NoteComment::isTopLevel)
                .map(NoteCommentDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 최상위 댓글만 페이징하여 조회
     * 권한: VIEW_REPORT 필요
     *
     * @param noteId 노트 ID
     * @param userId 조회 요청 사용자 ID
     * @param pageable 페이징 정보
     * @return 최상위 댓글 목록 (페이징)
     */
    public PageResponseDTO<NoteCommentDTO> getTopLevelComments(UUID noteId, UUID userId, Pageable pageable) {
        log.debug("최상위 댓글 조회 - noteId: {}, userId: {}, page: {}",
                noteId, userId, pageable.getPageNumber());

        Page<NoteComment> commentPage = commentRepository.findTopLevelByNoteIdWithAuth(
                noteId, userId, pageable);

        return PageResponseDTO.from(commentPage, NoteCommentDTO::fromWithoutReplies);
    }

    /**
     * 특정 댓글의 대댓글 목록 조회
     * 권한: VIEW_REPORT 필요
     *
     * @param parentCommentId 부모 댓글 ID
     * @param userId 조회 요청 사용자 ID
     * @return 대댓글 목록
     */
    public List<NoteCommentDTO> getReplies(UUID parentCommentId, UUID userId) {
        log.debug("대댓글 조회 - parentCommentId: {}, userId: {}", parentCommentId, userId);

        List<NoteComment> replies = commentRepository.findRepliesByParentIdWithAuth(
                parentCommentId, userId);

        return replies.stream()
                .map(NoteCommentDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 댓글 상세 조회
     * 권한: VIEW_REPORT 필요
     *
     * @param commentId 댓글 ID
     * @param userId 조회 요청 사용자 ID
     * @return 댓글 DTO
     */
    public NoteCommentDTO getComment(UUID commentId, UUID userId) {
        log.debug("댓글 조회 - commentId: {}, userId: {}", commentId, userId);

        NoteComment comment = commentRepository.findByIdWithAuth(commentId, userId)
                .orElseThrow(() -> new AccessDeniedException("댓글 조회 권한이 없거나 존재하지 않는 댓글입니다"));

        return NoteCommentDTO.from(comment);
    }

    // ============= 댓글 수정 =============

    /**
     * 댓글 수정
     * 권한: 작성자 본인만 수정 가능
     *
     * @param commentId 댓글 ID
     * @param dto 수정 요청 DTO
     * @param userId 수정 요청 사용자 ID
     * @return 수정된 댓글 DTO
     */
    @Transactional
    public NoteCommentDTO updateComment(UUID commentId, NoteCommentUpdateDTO dto, UUID userId) {
        log.info("댓글 수정 시작 - commentId: {}, userId: {}", commentId, userId);

        // 1. 댓글 조회 (권한 검증 포함)
        NoteComment comment = commentRepository.findByIdWithAuth(commentId, userId)
                .orElseThrow(() -> new AccessDeniedException("댓글 조회 권한이 없거나 존재하지 않는 댓글입니다"));

        // 2. 수정 권한 검증: 작성자 본인만
        if (!comment.canEdit(userId)) {
            log.warn("댓글 수정 권한 없음 - commentId: {}, userId: {}, authorId: {}",
                    commentId, userId, comment.getAuthor().getUserId());
            throw new AccessDeniedException("작성자 본인만 댓글을 수정할 수 있습니다");
        }

        // 3. 댓글 수정
        comment.changeContent(dto.getContent());

        log.info("댓글 수정 완료 - commentId: {}", commentId);
        return NoteCommentDTO.from(comment);
    }

    // ============= 댓글 삭제 =============

    /**
     * 댓글 삭제 (Soft Delete)
     * 권한: 작성자 본인 또는 주보호자
     *
     * @param commentId 댓글 ID
     * @param userId 삭제 요청 사용자 ID
     */
    @Transactional
    public void deleteComment(UUID commentId, UUID userId) {
        log.info("댓글 삭제 시작 - commentId: {}, userId: {}", commentId, userId);

        // 1. 댓글 조회 (권한 검증 포함)
        NoteComment comment = commentRepository.findByIdWithAuth(commentId, userId)
                .orElseThrow(() -> new AccessDeniedException("댓글 조회 권한이 없거나 존재하지 않는 댓글입니다"));

        // 2. 삭제 권한 검증: 작성자 본인 또는 주보호자
        if (!comment.canDelete(userId)) {
            log.warn("댓글 삭제 권한 없음 - commentId: {}, userId: {}", commentId, userId);
            throw new AccessDeniedException("작성자 본인 또는 주보호자만 댓글을 삭제할 수 있습니다");
        }

        // 3. Soft Delete (대댓글도 CASCADE로 함께 삭제됨)
        comment.delete();
        log.info("댓글 삭제 완료 - commentId: {}", commentId);
    }

    // ============= 통계 =============

    /**
     * 노트의 총 댓글 개수 (대댓글 포함)
     *
     * @param noteId 노트 ID
     * @return 댓글 개수
     */
    public long countCommentsByNote(UUID noteId) {
        return commentRepository.countByNoteId(noteId);
    }

    /**
     * 노트의 최상위 댓글 개수
     *
     * @param noteId 노트 ID
     * @return 최상위 댓글 개수
     */
    public long countTopLevelCommentsByNote(UUID noteId) {
        return commentRepository.countTopLevelByNoteId(noteId);
    }
}