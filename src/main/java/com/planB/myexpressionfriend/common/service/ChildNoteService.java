package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.note.NoteType;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import com.planB.myexpressionfriend.common.dto.note.*;
import com.planB.myexpressionfriend.common.repository.ChildNoteRepository;
import com.planB.myexpressionfriend.common.repository.ChildRepository;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
/**
 * ChildNote Service
 *
 * 책임:
 * - 노트 CRUD
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChildNoteService {

    private final ChildNoteRepository noteRepository;
    private final ChildRepository childRepository;
    private final UserRepository userRepository;

    // ============= 노트 생성 =============

    /**
     * 노트 생성
     *
     * @param dto 생성 요청 DTO
     */
    @Transactional
    public ChildNoteDTO createNote(ChildNoteCreateDTO dto, UUID authorId) {
        log.info("노트 생성 시작 - childId: {}, authorId: {}, type: {}", dto.getChildId(), authorId, dto.getType());

        dto.validateUserWritable();

        Child child = childRepository.findById(dto.getChildId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아동입니다."));

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));


        // role-based note type validation
        validateNoteTypeByRole(author, dto.getType());

        if (!child.hasPermission(authorId, ChildPermissionType.WRITE_NOTE)) {
            log.warn("노트 작성 권한 없음 - childId: {}, authorId: {}", dto.getChildId(), authorId);
            throw new AccessDeniedException("노트 작성 권한이 없습니다.");
        }

        // 5. 노트 생성
        ChildNote note = ChildNote.builder()
                .child(child)
                .author(author)
                .type(dto.getType())
                .title(dto.getTitle())
                .content(dto.getContent())
                .isDeleted(false)
                .build();

        ChildNote savedNote = noteRepository.save(note);
        log.info("노트 생성 완료 - noteId: {}", savedNote.getNoteId());

        return ChildNoteDTO.from(savedNote);

    }

    private void validateNoteTypeByRole(User author, NoteType type) {
        if (type == null) {
            throw new IllegalArgumentException("노트 타입은 필수입니다.");
        }
        if (type == NoteType.PARENT_NOTE && !author.hasRole(UserRole.PARENT)) {
            throw new AccessDeniedException("노트 조회 권한이 없거나 존재하지 않는 노트입니다.");
        }
        if (type == NoteType.THERAPIST_NOTE
                && !(author.hasRole(UserRole.THERAPIST) || author.hasRole(UserRole.TEACHER))) {
            throw new AccessDeniedException("노트 조회 권한이 없거나 존재하지 않는 노트입니다.");
        }
    }

    /**
     *
     * @param childId 아동 ID
     * @param title 제목
     */
    @Transactional
    public ChildNoteDTO createSystemNote(UUID childId, UUID authorId, String title, String content) {
        ChildNote savedNote = createSystemNoteEntity(childId, authorId, title, content, NoteType.SYSTEM);
        return ChildNoteDTO.from(savedNote);
    }

    @Transactional
    public ChildNote createSystemNoteEntity(
            UUID childId,
            UUID authorId,
            String title,
            String content,
            NoteType systemType
    ) {
        log.info("시스템 노트 생성 - childId: {}, authorId: {}", childId, authorId);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아동입니다."));

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("본문은 필수입니다.");
        }
        NoteType resolvedType = systemType == null ? NoteType.SYSTEM : systemType;
        if (!resolvedType.isSystemGenerated()) {
            throw new IllegalArgumentException("SYSTEM 노트 타입만 허용됩니다.");
        }

        ChildNote note = ChildNote.builder()
                .child(child)
                .author(author)
                .type(resolvedType)
                .title(title)
                .content(content)
                .isDeleted(false)
                .build();

        ChildNote savedNote = noteRepository.save(note);
        log.info("시스템 노트 생성 완료 - noteId: {}", savedNote.getNoteId());
        return savedNote;
    }


    /**
     *
     * @param noteId 노트 ID
     * @return 노트 상세 DTO
     */
    public ChildNoteDetailDTO getNote(UUID noteId, UUID userId) {
        log.debug("노트 조회 - noteId: {}, userId: {}", noteId, userId);

        ChildNote note = noteRepository.findByIdWithAuth(noteId, userId)
                .orElseThrow(() -> new AccessDeniedException("노트 조회 권한이 없거나 존재하지 않는 노트입니다."));

        return ChildNoteDetailDTO.from(note);
    }

    /**
     *
     * @param childId 아동 ID
     * @return 노트 목록(페이지)
     */
    public PageResponseDTO<ChildNoteDTO> getNotesByChild(UUID childId, UUID userId, Pageable pageable) {
        log.debug("아동 노트 목록 조회 - childId: {}, userId: {}, page: {}", childId, userId, pageable.getPageNumber());

        Page<ChildNote> notePage = noteRepository.findByChildIdWithAuth(childId, userId, pageable);

        return PageResponseDTO.from(notePage, ChildNoteDTO::from);
    }

    /**
     */
    public PageResponseDTO<ChildNoteDTO> getAccessibleNotes(
            UUID userId,
            NoteType type,
            String keyword,
            Pageable pageable
    ) {
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        boolean hasKeyword = normalizedKeyword != null && !normalizedKeyword.isBlank();

        Page<ChildNote> notePage;
        if (type != null && hasKeyword) {
            notePage = noteRepository.searchAllAccessibleByUserIdAndType(userId, type, normalizedKeyword, pageable);
        } else if (type != null) {
            notePage = noteRepository.findAllAccessibleByUserIdAndType(userId, type, pageable);
        } else if (hasKeyword) {
            notePage = noteRepository.searchAllAccessibleByUserId(userId, normalizedKeyword, pageable);
        } else {
            notePage = noteRepository.findAllAccessibleByUserId(userId, pageable);
        }

        return PageResponseDTO.from(notePage, ChildNoteDTO::from);
    }

    /**
     */
    public PageResponseDTO<ChildNoteDTO> searchNotes(NoteSearchDTO searchDTO, UUID userId) {
        log.debug("노트 검색 - childId: {}, userId: {}, keyword: {}", searchDTO.getChildId(), userId, searchDTO.getKeyword());

        searchDTO.validate();
        Pageable pageable = searchDTO.toPageable();

        Page<ChildNote> notePage;

        if (searchDTO.getKeyword() != null && !searchDTO.getKeyword().isBlank()) {
            notePage = noteRepository.searchByKeywordWithAuth(
                    searchDTO.getChildId(),
                    userId,
                    searchDTO.getKeyword(),
                    pageable
            );
        } else if (searchDTO.getType() != null) {
            // 타입별 필터링
            notePage = noteRepository.findByChildIdAndTypeWithAuth(
                    searchDTO.getChildId(),
                    userId,
                    searchDTO.getType(),
                    pageable
            );
        } else if (searchDTO.getAuthorId() != null) {
            // 작성자별 필터링
            notePage = noteRepository.findByChildIdAndAuthorWithAuth(
                    searchDTO.getChildId(),
                    userId,
                    searchDTO.getAuthorId(),
                    pageable
            );
        } else if (searchDTO.getStartDate() != null && searchDTO.getEndDate() != null) {
            notePage = noteRepository.findByChildIdAndDateRangeWithAuth(
                    searchDTO.getChildId(),
                    userId,
                    searchDTO.getStartDate(),
                    searchDTO.getEndDate(),
                    pageable
            );
        } else {
            notePage = noteRepository.findByChildIdWithAuth(
                    searchDTO.getChildId(),
                    userId,
                    pageable
            );
        }

        return PageResponseDTO.from(notePage, ChildNoteDTO::from);
    }

    // ============= 노트 수정 =============

    /**
     * 노트 수정
     *
     * @param noteId 노트 ID
     * @param dto 수정 요청 DTO
     */
    @Transactional
    public ChildNoteDTO updateNote(UUID noteId, ChildNoteUpdateDTO dto, UUID userId) {
        log.info("노트 수정 시작 - noteId: {}, userId: {}", noteId, userId);

        ChildNote note = noteRepository.findByIdWithAuth(noteId, userId)
                .orElseThrow(() -> new AccessDeniedException("노트 조회 권한이 없거나 존재하지 않는 노트입니다."));

        if (!note.canEdit(userId)) {
            log.warn("노트 수정 권한 없음 - noteId: {}, userId: {}, authorId: {}", noteId, userId, note.getAuthor().getUserId());
            throw new AccessDeniedException("작성자 본인만 노트를 수정할 수 있습니다.");
        }

        // 3. 노트 수정
        if (dto.getTitle() != null) {
            note.changeTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            note.changeContent(dto.getContent());
        }

        log.info("노트 수정 완료 - noteId: {}", noteId);
        return ChildNoteDTO.from(note);
    }


    /**
     *
     * @param noteId 노트 ID
     */
    @Transactional
    public void deleteNote(UUID noteId, UUID userId) {
        log.info("노트 삭제 시작 - noteId: {}, userId: {}", noteId, userId);

        ChildNote note = noteRepository.findByIdWithAuth(noteId, userId)
                .orElseThrow(() -> new AccessDeniedException("노트 조회 권한이 없거나 존재하지 않는 노트입니다."));

        if (!note.canDelete(userId)) {
            log.warn("노트 삭제 권한 없음 - noteId: {}, userId: {}", noteId, userId);
            throw new AccessDeniedException("노트 조회 권한이 없거나 존재하지 않는 노트입니다.");
        }

        // 3. Soft Delete
        note.delete();
        log.info("노트 삭제 완료 - noteId: {}", noteId);
    }

    // ============= 통계 =============

    /**
     *
     * @param childId 아동 ID
     */
    public long countNotesByChild(UUID childId, UUID userId) {
        return noteRepository.countByChildIdWithAuth(childId, userId);
    }

    /**
     *
     * @param childId 아동 ID
     */
    public long countNotesByChildAndType(
            UUID childId,
            UUID userId,
            com.planB.myexpressionfriend.common.domain.note.NoteType type
    ) {
        return noteRepository.countByChildIdAndTypeWithAuth(childId, userId, type);
    }
}
