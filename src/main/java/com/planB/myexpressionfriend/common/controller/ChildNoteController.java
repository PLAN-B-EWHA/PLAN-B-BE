package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.domain.note.NoteType;
import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.note.*;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.service.ChildNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.planB.myexpressionfriend.common.util.PageableUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ChildNote Controller
 *
 * 노트 CRUD, 검색, 통계 API
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "ChildNote", description = "치료 노트 API")
public class ChildNoteController {

    private final ChildNoteService noteService;

    // ============= 노트 생성 =============
    @PostMapping("/children/{childId}/notes")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "노트 생성", description = "새로운 치료 노트를 작성합니다. WRITE_NOTE 권한이 필요합니다.")
    public ResponseEntity<ApiResponse<ChildNoteDTO>> createNote(
            @PathVariable UUID childId,
            @Valid @RequestBody ChildNoteCreateDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
            ) {
        log.info("POST /api/children/{}/notes - userId: {}", childId, currentUser.getUserId());

        // childId 일치 검증
        if (!dto.getChildId().equals(childId)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            "경로의 childId와 요청 본문의 childId가 일치하지 않습니다",
                            "INVALID_PATH_PARAM"
                    ));
        }

        ChildNoteDTO note = noteService.createNote(dto, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("노트가 생성되었습니다",note));
    }

    // ============= 노트 조회 =============

    @GetMapping("/notes/{noteId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "노트 조회", description = "특정 노트의 상세 정보를 조회합니다. VIEW_REPORT 권한이 필요합니다.")
    public ResponseEntity<ApiResponse<ChildNoteDetailDTO>> getNote(
            @PathVariable UUID noteId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("GET /api/notes/{} - userId: {}", noteId, currentUser.getUserId());

        ChildNoteDetailDTO note = noteService.getNote(noteId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(note));
    }

    @GetMapping("/children/{childId}/notes")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "노트 목록 조회", description = "특정 아동의 노트 목록을 페이징하여 조회합니다")
    public ResponseEntity<ApiResponse<PageResponseDTO<ChildNoteDTO>>> getNotesByChild(
            @PathVariable UUID childId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("GET /api/children/{}/notes - userId: {}, page: {}",
                childId, currentUser.getUserId(), page);

        Pageable pageable = PageableUtil.createPageable(page, size, sortBy, sortDirection, Sort.Direction.DESC);

        PageResponseDTO<ChildNoteDTO> notes = noteService.getNotesByChild(
                childId, currentUser.getUserId(), pageable
        );

        return ResponseEntity.ok(ApiResponse.success(notes));
    }

    // ============= 노트 검색 =============

    @GetMapping("/children/{childId}/notes/search")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "노트 검색", description = "키워드, 타입, 작성자, 날짜 범위로 노트를 검색합니다.")
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
        log.info("GET /api/children/{}/notes/search - userId: {}, keyword: {}",
                childId, currentUser.getUserId(), keyword);


        // NoteSearchDTO 생성
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

        PageResponseDTO<ChildNoteDTO> notes = noteService.searchNotes(
                searchDTO, currentUser.getUserId()
        );

        return ResponseEntity.ok(ApiResponse.success(notes));
    }

    // ============= 노트 수정 =============

    @PutMapping("/notes/{noteId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "노트 수정", description = "노트를 수정합니다. 작성자 본인만 수정 가능합니다.")
    public ResponseEntity<ApiResponse<ChildNoteDTO>> updateNote(
            @PathVariable UUID noteId,
            @Valid @RequestBody ChildNoteUpdateDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("PUT /api/notes/{} - userId: {}", noteId, currentUser.getUserId());

        ChildNoteDTO note = noteService.updateNote(noteId, dto, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("노트가 수정되었습니다", note));
    }

    // ============= 노트 삭제 =============

    @DeleteMapping("/notes/{noteId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "노트 삭제", description = "노트를 삭제합니다. 작성자 본인 또는 주보호자만 삭제 가능합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteNote(
            @PathVariable UUID noteId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("DELETE /api/notes/{} - userId: {}", noteId, currentUser.getUserId());

        noteService.deleteNote(noteId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("노트가 삭제되었습니다"));
    }

    // ============= 통계 =============

    @GetMapping("/children/{childId}/notes/count")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "노트 개수 조회", description = "특정 아동의 노트 개수를 조회합니다.")
    public ResponseEntity<ApiResponse<Long>> countNotes(
            @PathVariable UUID childId,
            @RequestParam(required = false) com.planB.myexpressionfriend.common.domain.note.NoteType type,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("GET /api/children/{}/notes/count - userId: {}, type: {}",
                childId, currentUser.getUserId(), type);

        long count;
        if (type != null) {
            count = noteService.countNotesByChildAndType(childId, currentUser.getUserId(), type);
        } else {
            count = noteService.countNotesByChild(childId, currentUser.getUserId());
        }

        return ResponseEntity.ok(ApiResponse.success(count));
    }

}
