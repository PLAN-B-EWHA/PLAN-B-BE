package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.user.User;
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
 * - 권한 검증 (WRITE_NOTE, VIEW_REPORT)
 * - 비즈니스 로직 처리
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
     * 권한: WRITE_NOTE 필요
     *
     * @param dto 생성 요청 DTO
     * @param authorId 작성자 ID
     * @return 생성된 노트 DTO
     */
    @Transactional
    public ChildNoteDTO createNote(ChildNoteCreateDTO dto, UUID authorId) {
        log.info("노트 생성 시작 - childId: {}, authorId: {}, type: {}", dto.getChildId(), authorId, dto.getType());

        // 1. DTO 유효성 검증
        dto.validateUserWritable();

        // 2. 아동 조회
        Child child = childRepository.findById(dto.getChildId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아동입니다"));

        // 3. 작성자 조회
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));

        // 4. 권한 검증: WRITE_NOTE 필요
        if (!child.hasPermission(authorId, ChildPermissionType.WRITE_NOTE)) {
            log.warn("노트 작성 권한 없음 - childId: {}, authorId: {}", dto.getChildId(), authorId);
            throw new AccessDeniedException("노트 작성 권한이 없습니다");
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

        // 6. 저장
        ChildNote savedNote = noteRepository.save(note);
        log.info("노트 생성 완료 - noteId: {}", savedNote.getNoteId());

        return ChildNoteDTO.from(savedNote);

    }

    /**
     * SYSTEM 타입 노트 생성 (내부용)
     * 권한 검증 없음 (시스템이 자동 생성)
     *
     * @param childId 아동 ID
     * @param authorId 작성자 ID (시스템 사용자)
     * @param title 제목
     * @param content 본문
     * @return 생성된 노트 DTO
     */
    @Transactional
    public ChildNoteDTO createSystemNote(UUID childId, UUID authorId, String title, String content) {
        log.info("시스템 노트 생성 - childId: {}, authorId: {}", childId, authorId);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아동입니다"));

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));

        ChildNote note = ChildNote.builder()
                .child(child)
                .author(author)
                .type(com.planB.myexpressionfriend.common.domain.note.NoteType.SYSTEM)
                .title(title)
                .content(content)
                .isDeleted(false)
                .build();

        ChildNote savedNote = noteRepository.save(note);
        log.info("시스템 노트 생성 완료 - noteId: {}", savedNote.getNoteId());

        return ChildNoteDTO.from(savedNote);
    }

    // ============= 노트 조회 =============

    /**
     * 노트 상세 조회
     * 권한: VIEW_REPORT 필요
     *
     * @param noteId 노트 ID
     * @param userId 조회 요청 사용자 ID
     * @return 노트 상세 DTO
     */
    public ChildNoteDetailDTO getNote(UUID noteId, UUID userId) {
        log.debug("노트 조회 - noteId: {}, userId: {}", noteId, userId);

        ChildNote note = noteRepository.findByIdWithAuth(noteId, userId)
                .orElseThrow(() -> new AccessDeniedException("노트 조회 권한이 없거나 존재하지 않는 노트입니다"));

        return ChildNoteDetailDTO.from(note);
    }

    /**
     * 아동의 노트 목록 조회 (페이징)
     * 권한: VIEW_REPORT 필요
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @param pageable 페이징 정보
     * @return 노트 목록 (페이징)
     */
    public PageResponseDTO<ChildNoteDTO> getNotesByChild(UUID childId, UUID userId, Pageable pageable) {
        log.debug("아동 노트 목록 조회 - childId: {}, userId: {}, page: {}",
                childId, userId, pageable.getPageNumber());

        // 권한 검증은 Repository 쿼리에서 수행됨
        Page<ChildNote> notePage = noteRepository.findByChildIdWithAuth(childId, userId, pageable);

        return PageResponseDTO.from(notePage, ChildNoteDTO::from);
    }

    /**
     * 노트 검색/필터링
     * 권한: VIEW_REPORT 필요
     *
     * @param searchDTO 검색 조건
     * @param userId 조회 요청 사용자 ID
     * @return 검색 결과 (페이징)
     */
    public PageResponseDTO<ChildNoteDTO> searchNotes(NoteSearchDTO searchDTO, UUID userId) {
        log.debug("노트 검색 - childId: {}, userId: {}, keyword: {}",
                searchDTO.getChildId(), userId, searchDTO.getKeyword());

        searchDTO.validate();
        Pageable pageable = searchDTO.toPageable();

        Page<ChildNote> notePage;

        // 검색 조건에 따라 다른 Repository 메서드 호출
        if (searchDTO.getKeyword() != null && !searchDTO.getKeyword().isBlank()) {
            // 키워드 검색
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
            // 날짜 범위 필터링
            notePage = noteRepository.findByChildIdAndDateRangeWithAuth(
                    searchDTO.getChildId(),
                    userId,
                    searchDTO.getStartDate(),
                    searchDTO.getEndDate(),
                    pageable
            );
        } else {
            // 조건 없음 - 전체 조회
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
     * 권한: 작성자 본인만 수정 가능
     *
     * @param noteId 노트 ID
     * @param dto 수정 요청 DTO
     * @param userId 수정 요청 사용자 ID
     * @return 수정된 노트 DTO
     */
    @Transactional
    public ChildNoteDTO updateNote(UUID noteId, ChildNoteUpdateDTO dto, UUID userId) {
        log.info("노트 수정 시작 - noteId: {}, userId: {}", noteId, userId);

        // 1. 노트 조회 (권한 검증 포함)
        ChildNote note = noteRepository.findByIdWithAuth(noteId, userId)
                .orElseThrow(() -> new AccessDeniedException("노트 조회 권한이 없거나 존재하지 않는 노트입니다"));

        // 2. 수정 권한 검증: 작성자 본인만
        if (!note.canEdit(userId)) {
            log.warn("노트 수정 권한 없음 - noteId: {}, userId: {}, authorId: {}",
                    noteId, userId, note.getAuthor().getUserId());
            throw new AccessDeniedException("작성자 본인만 노트를 수정할 수 있습니다");
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

    // ============= 노트 삭제 =============

    /**
     * 노트 삭제 (Soft Delete)
     * 권한: 작성자 본인 또는 주보호자
     *
     * @param noteId 노트 ID
     * @param userId 삭제 요청 사용자 ID
     */
    @Transactional
    public void deleteNote(UUID noteId, UUID userId) {
        log.info("노트 삭제 시작 - noteId: {}, userId: {}", noteId, userId);

        // 1. 노트 조회 (권한 검증 포함)
        ChildNote note = noteRepository.findByIdWithAuth(noteId, userId)
                .orElseThrow(() -> new AccessDeniedException("노트 조회 권한이 없거나 존재하지 않는 노트입니다"));

        // 2. 삭제 권한 검증: 작성자 본인 또는 주보호자
        if (!note.canDelete(userId)) {
            log.warn("노트 삭제 권한 없음 - noteId: {}, userId: {}", noteId, userId);
            throw new AccessDeniedException("작성자 본인 또는 주보호자만 노트를 삭제할 수 있습니다");
        }

        // 3. Soft Delete
        note.delete();
        log.info("노트 삭제 완료 - noteId: {}", noteId);
    }

    // ============= 통계 =============

    /**
     * 아동의 노트 개수 조회
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @return 노트 개수
     */
    public long countNotesByChild(UUID childId, UUID userId) {
        return noteRepository.countByChildIdWithAuth(childId, userId);
    }

    /**
     * 아동의 노트 타입별 개수 조회
     *
     * @param childId 아동 ID
     * @param userId 조회 요청 사용자 ID
     * @param type 노트 타입
     * @return 노트 개수
     */
    public long countNotesByChildAndType(
            UUID childId,
            UUID userId,
            com.planB.myexpressionfriend.common.domain.note.NoteType type
    ) {
        return noteRepository.countByChildIdAndTypeWithAuth(childId, userId, type);
    }
}
