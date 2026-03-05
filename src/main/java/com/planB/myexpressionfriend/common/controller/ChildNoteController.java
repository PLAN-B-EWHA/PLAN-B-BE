package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.domain.note.NoteType;
import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.note.ChildNoteCreateDTO;
import com.planB.myexpressionfriend.common.dto.note.ChildNoteDTO;
import com.planB.myexpressionfriend.common.dto.note.ChildNoteDetailDTO;
import com.planB.myexpressionfriend.common.dto.note.ChildNoteUpdateDTO;
import com.planB.myexpressionfriend.common.dto.note.NoteSearchDTO;
import com.planB.myexpressionfriend.common.dto.note.PageResponseDTO;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.service.ChildNoteService;
import com.planB.myexpressionfriend.common.util.PageableUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 노트 Controller
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "ChildNote", description = "노트 API")
public class ChildNoteController {

    private final ChildNoteService noteService;

    @PostMapping("/children/{childId}/notes")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    public ResponseEntity<ApiResponse<ChildNoteDTO>> createNote(
            @PathVariable UUID childId,
            @Valid @RequestBody ChildNoteCreateDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        if (!dto.getChildId().equals(childId)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("경로 childId와 본문 childId가 일치하지 않습니다.", "INVALID_PATH_PARAM"));
        }

        ChildNoteDTO note = noteService.createNote(dto, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("노트가 생성되었습니다.", note));
    }

    @GetMapping("/notes/{noteId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    public ResponseEntity<ApiResponse<ChildNoteDetailDTO>> getNote(
            @PathVariable UUID noteId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        ChildNoteDetailDTO note = noteService.getNote(noteId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(note));
    }

    @GetMapping("/children/{childId}/notes")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    public ResponseEntity<ApiResponse<PageResponseDTO<ChildNoteDTO>>> getNotesByChild(
            @PathVariable UUID childId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        Pageable pageable = PageableUtil.createPageable(page, size, sortBy, sortDirection, Sort.Direction.DESC);
        PageResponseDTO<ChildNoteDTO> notes = noteService.getNotesByChild(childId, currentUser.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(notes));
    }

    @GetMapping("/notes/feed")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "통합 노트 피드 조회", description = "권한이 있는 전체 아동 노트를 최신순으로 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<ChildNoteDTO>>> getNoteFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) NoteType type,
            @RequestParam(required = false) String keyword,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        Pageable pageable = PageableUtil.createPageable(page, size, sortBy, sortDirection, Sort.Direction.DESC);
        PageResponseDTO<ChildNoteDTO> notes =
                noteService.getAccessibleNotes(currentUser.getUserId(), type, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(notes));
    }

    @GetMapping("/children/{childId}/notes/search")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    public ResponseEntity<ApiResponse<PageResponseDTO<ChildNoteDTO>>> searchNotes(
            @PathVariable UUID childId,
            @RequestParam(required = false) NoteType type,
            @RequestParam(required = false) UUID authorId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        NoteSearchDTO searchDTO = NoteSearchDTO.builder()
                .childId(childId)
                .type(type)
                .authorId(authorId)
                .keyword(keyword)
                .startDate(startDate)
                .endDate(endDate)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponseDTO<ChildNoteDTO> notes = noteService.searchNotes(searchDTO, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(notes));
    }

    @PutMapping("/notes/{noteId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    public ResponseEntity<ApiResponse<ChildNoteDTO>> updateNote(
            @PathVariable UUID noteId,
            @Valid @RequestBody ChildNoteUpdateDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        ChildNoteDTO note = noteService.updateNote(noteId, dto, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("노트가 수정되었습니다.", note));
    }

    @DeleteMapping("/notes/{noteId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    public ResponseEntity<ApiResponse<Void>> deleteNote(
            @PathVariable UUID noteId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        noteService.deleteNote(noteId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("노트가 삭제되었습니다."));
    }

    @GetMapping("/children/{childId}/notes/count")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    public ResponseEntity<ApiResponse<Long>> countNotes(
            @PathVariable UUID childId,
            @RequestParam(required = false) NoteType type,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        long count = type != null
                ? noteService.countNotesByChildAndType(childId, currentUser.getUserId(), type)
                : noteService.countNotesByChild(childId, currentUser.getUserId());

        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
