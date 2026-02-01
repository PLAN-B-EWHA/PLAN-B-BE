package com.planB.myexpressionfriend.common.controller;


import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.note.*;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.service.NoteCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * NoteComment Controller
 *
 * 댓글/대댓글 CRUD API
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "NoteComment", description = "노트 댓글 API")
public class NoteCommentController {

    private final NoteCommentService commentService;

    // ============= 댓글 생성 =============

    @PostMapping("/notes/{noteId}/comments")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "댓글 작성", description = "노트에 댓글을 작성합니다. VIEW_REPORT 권한이 필요합니다.")
    public ResponseEntity<ApiResponse<NoteCommentDTO>> createComment(
            @PathVariable UUID noteId,
            @Valid @RequestBody NoteCommentCreateDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("POST /api/notes/{}/comments - userId: {}", noteId, currentUser.getUserId());

        // noteId 일치 검증
        if (!dto.getNoteId().equals(noteId)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            "경로의 noteId와 요청 본문의 noteId가 일치하지 않습니다",
                            "INVALID_PATH_PARAM"
                    ));
        }

        NoteCommentDTO comment = commentService.createComment(dto, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("댓글이 작성되었습니다", comment));
    }

    // ============= 댓글 조회 =============

    @GetMapping("/notes/{noteId}/comments")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "댓글 목록 조회", description = "노트의 모든 댓글을 조회합니다. (계층 구조)")
    public ResponseEntity<ApiResponse<List<NoteCommentDTO>>> getCommentsByNote(
            @PathVariable UUID noteId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("GET /api/notes/{}/comments - userId: {}", noteId, currentUser.getUserId());

        List<NoteCommentDTO> comments = commentService.getCommentsByNote(
                noteId, currentUser.getUserId());

        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @GetMapping("/notes/{noteId}/comments/top-level")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "최상위 댓글 목록 조회", description = "최상위 댓글만 페이징하여 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<NoteCommentDTO>>> getTopLevelComments(
            @PathVariable UUID noteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("GET /api/notes/{}/comments/top-level - userId: {}, page: {}",
                noteId, currentUser.getUserId(), page);

        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        PageResponseDTO<NoteCommentDTO> comments = commentService.getTopLevelComments(
                noteId, currentUser.getUserId(), pageable);

        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @GetMapping("/comments/{commentId}/replies")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "대댓글 목록 조회", description = "특정 댓글의 대댓글 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<NoteCommentDTO>>> getReplies(
            @PathVariable UUID commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("GET /api/comments/{}/replies - userId: {}", commentId, currentUser.getUserId());

        List<NoteCommentDTO> replies = commentService.getReplies(
                commentId, currentUser.getUserId());

        return ResponseEntity.ok(ApiResponse.success(replies));
    }

    @GetMapping("/comments/{commentId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "댓글 상세 조회", description = "특정 댓글의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<NoteCommentDTO>> getComment(
            @PathVariable UUID commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("GET /api/comments/{} - userId: {}", commentId, currentUser.getUserId());

        NoteCommentDTO comment = commentService.getComment(commentId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(comment));
    }

    // ============= 댓글 수정 =============

    @PutMapping("/comments/{commentId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "댓글 수정", description = "댓글을 수정합니다. 작성자 본인만 수정 가능합니다.")
    public ResponseEntity<ApiResponse<NoteCommentDTO>> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody NoteCommentUpdateDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("PUT /api/comments/{} - userId: {}", commentId, currentUser.getUserId());

        NoteCommentDTO comment = commentService.updateComment(
                commentId, dto, currentUser.getUserId());

        return ResponseEntity.ok(ApiResponse.success("댓글이 수정되었습니다", comment));
    }

    // ============= 댓글 삭제 =============

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다. 작성자 본인 또는 주보호자만 삭제 가능합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        log.info("DELETE /api/comments/{} - userId: {}", commentId, currentUser.getUserId());

        commentService.deleteComment(commentId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("댓글이 삭제되었습니다"));
    }

    // ============= 통계 =============

    @GetMapping("/notes/{noteId}/comments/count")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "댓글 개수 조회", description = "노트의 총 댓글 개수를 조회합니다.")
    public ResponseEntity<ApiResponse<Long>> countComments(
            @PathVariable UUID noteId,
            @RequestParam(defaultValue = "false") boolean topLevelOnly
    ) {
        log.info("GET /api/notes/{}/comments/count - topLevelOnly: {}", noteId, topLevelOnly);

        long count = topLevelOnly
                ? commentService.countTopLevelCommentsByNote(noteId)
                : commentService.countCommentsByNote(noteId);

        return ResponseEntity.ok(ApiResponse.success(count));
    }
}