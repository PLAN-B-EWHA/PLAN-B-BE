package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.note.NoteComment;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.dto.note.NoteCommentCreateDTO;
import com.planB.myexpressionfriend.common.dto.note.NoteCommentDTO;
import com.planB.myexpressionfriend.common.dto.note.NoteCommentUpdateDTO;
import com.planB.myexpressionfriend.common.dto.note.PageResponseDTO;
import com.planB.myexpressionfriend.common.event.NoteCommentCreatedEvent;
import com.planB.myexpressionfriend.common.repository.ChildNoteRepository;
import com.planB.myexpressionfriend.common.repository.NoteCommentRepository;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NoteCommentService {

    private final NoteCommentRepository commentRepository;
    private final ChildNoteRepository noteRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public NoteCommentDTO createComment(NoteCommentCreateDTO dto, UUID authorId) {
        log.info("Comment create start - noteId: {}, authorId: {}, isReply: {}",
                dto.getNoteId(), authorId, dto.getParentCommentId() != null);

        ChildNote note = noteRepository.findByIdWithAuth(dto.getNoteId(), authorId)
                .orElseThrow(() -> new AccessDeniedException("노트 조회 권한이 없거나 존재하지 않는 노트입니다."));

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Child child = note.getChild();
        if (!child.hasPermission(authorId, ChildPermissionType.WRITE_NOTE)) {
            throw new AccessDeniedException("댓글 작성 권한이 없습니다.");
        }

        NoteComment parentComment = null;
        if (dto.getParentCommentId() != null) {
            parentComment = commentRepository.findByIdWithAuth(dto.getParentCommentId(), authorId)
                    .orElseThrow(() -> new AccessDeniedException("부모 댓글 조회 권한이 없거나 존재하지 않는 댓글입니다."));

            if (!parentComment.isTopLevel()) {
                throw new IllegalArgumentException("대댓글은 최상위 댓글에만 작성할 수 있습니다.");
            }
        }

        NoteComment comment = NoteComment.builder()
                .note(note)
                .author(author)
                .parentComment(parentComment)
                .content(dto.getContent())
                .isDeleted(false)
                .build();

        note.addComment(comment);
        if (parentComment != null) {
            parentComment.addReply(comment);
        }

        NoteComment savedComment = commentRepository.save(comment);
        log.info("Comment created - commentId: {}", savedComment.getCommentId());

        eventPublisher.publishEvent(new NoteCommentCreatedEvent(
                child.getChildId(),
                note.getNoteId(),
                savedComment.getCommentId(),
                authorId,
                parentComment != null
        ));

        return NoteCommentDTO.from(savedComment);
    }

    public List<NoteCommentDTO> getCommentsByNote(UUID noteId, UUID userId) {
        List<NoteComment> comments = commentRepository.findByNoteIdWithAuth(noteId, userId);
        return comments.stream()
                .filter(NoteComment::isTopLevel)
                .map(NoteCommentDTO::from)
                .collect(Collectors.toList());
    }

    public PageResponseDTO<NoteCommentDTO> getTopLevelComments(UUID noteId, UUID userId, Pageable pageable) {
        Page<NoteComment> commentPage = commentRepository.findTopLevelByNoteIdWithAuth(noteId, userId, pageable);
        return PageResponseDTO.from(commentPage, NoteCommentDTO::fromWithoutReplies);
    }

    public List<NoteCommentDTO> getReplies(UUID parentCommentId, UUID userId) {
        List<NoteComment> replies = commentRepository.findRepliesByParentIdWithAuth(parentCommentId, userId);
        return replies.stream()
                .map(NoteCommentDTO::from)
                .collect(Collectors.toList());
    }

    public NoteCommentDTO getComment(UUID commentId, UUID userId) {
        NoteComment comment = commentRepository.findByIdWithAuth(commentId, userId)
                .orElseThrow(() -> new AccessDeniedException("댓글 조회 권한이 없거나 존재하지 않는 댓글입니다."));
        return NoteCommentDTO.from(comment);
    }

    @Transactional
    public NoteCommentDTO updateComment(UUID commentId, NoteCommentUpdateDTO dto, UUID userId) {
        NoteComment comment = commentRepository.findByIdWithAuth(commentId, userId)
                .orElseThrow(() -> new AccessDeniedException("댓글 조회 권한이 없거나 존재하지 않는 댓글입니다."));

        if (!comment.canEdit(userId)) {
            throw new AccessDeniedException("작성자만 댓글을 수정할 수 있습니다.");
        }

        comment.changeContent(dto.getContent());
        return NoteCommentDTO.from(comment);
    }

    @Transactional
    public void deleteComment(UUID commentId, UUID userId) {
        NoteComment comment = commentRepository.findByIdWithAuth(commentId, userId)
                .orElseThrow(() -> new AccessDeniedException("댓글 조회 권한이 없거나 존재하지 않는 댓글입니다."));

        if (!comment.canDelete(userId)) {
            throw new AccessDeniedException("작성자 또는 부모만 댓글을 삭제할 수 있습니다.");
        }

        comment.delete();
    }

    public long countCommentsByNote(UUID noteId) {
        return commentRepository.countByNoteId(noteId);
    }

    public long countTopLevelCommentsByNote(UUID noteId) {
        return commentRepository.countTopLevelByNoteId(noteId);
    }
}
