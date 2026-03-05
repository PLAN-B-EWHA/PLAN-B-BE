package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.note.NoteCommentCreateDTO;
import com.planB.myexpressionfriend.common.dto.note.NoteCommentDTO;
import com.planB.myexpressionfriend.common.dto.note.NoteCommentUpdateDTO;
import com.planB.myexpressionfriend.common.dto.note.PageResponseDTO;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.service.NoteCommentService;
import com.planB.myexpressionfriend.common.util.PageableUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import java.util.List;
import java.util.UUID;

/**
 * 노트 댓글 Controller
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "NoteComment", description = "노트 댓글 API")
public class NoteCommentController {

    private final NoteCommentService commentService;

    @PostMapping("/notes/{noteId}/comments")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "댓글 작성", description = "노트에 댓글을 작성합니다. VIEW_REPORT 권한이 필요합니다.")
    public ResponseEntity<ApiResponse<NoteCommentDTO>> createComment(
            @PathVariable UUID noteId,
            @Valid @RequestBody NoteCommentCreateDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        if (!dto.getNoteId().equals(noteId)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("경로 noteId와 본문 noteId가 일치하지 않습니다.", "INVALID_PATH_PARAM"));
        }

        NoteCommentDTO comment = commentService.createComment(dto, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("댓글이 작성되었습니다.", comment));
    }

    @GetMapping("/notes/{noteId}/comments")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "댓글 목록 조회", description = "노트의 모든 댓글을 트리 구조로 조회합니다.")
    public ResponseEntity<ApiResponse<List<NoteCommentDTO>>> getCommentsByNote(
            @PathVariable UUID noteId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        List<NoteCommentDTO> comments = commentService.getCommentsByNote(noteId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @GetMapping("/notes/{noteId}/comments/top-level")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "최상위 댓글 목록 조회", description = "최상위 댓글만 페이지네이션하여 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponseDTO<NoteCommentDTO>>> getTopLevelComments(
            @PathVariable UUID noteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        Pageable pageable = PageableUtil.createPageable(page, size, sortBy, sortDirection, Sort.Direction.ASC);
        PageResponseDTO<NoteCommentDTO> comments =
                commentService.getTopLevelComments(noteId, currentUser.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @GetMapping("/comments/{commentId}/replies")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "대댓글 목록 조회", description = "특정 댓글의 대댓글 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<NoteCommentDTO>>> getReplies(
            @PathVariable UUID commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        List<NoteCommentDTO> replies = commentService.getReplies(commentId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(replies));
    }

    @GetMapping("/comments/{commentId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "댓글 상세 조회", description = "특정 댓글의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<NoteCommentDTO>> getComment(
            @PathVariable UUID commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        NoteCommentDTO comment = commentService.getComment(commentId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(comment));
    }

    @PutMapping("/comments/{commentId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "댓글 수정", description = "댓글을 수정합니다. 작성자 본인만 수정 가능합니다.")
    public ResponseEntity<ApiResponse<NoteCommentDTO>> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody NoteCommentUpdateDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        NoteCommentDTO comment = commentService.updateComment(commentId, dto, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("댓글이 수정되었습니다.", comment));
    }

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다. 작성자 본인 또는 부모만 삭제 가능합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDTO currentUser
    ) {
        commentService.deleteComment(commentId, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("댓글이 삭제되었습니다."));
    }

    @GetMapping("/notes/{noteId}/comments/count")
    @PreAuthorize("hasAnyRole('PARENT', 'THERAPIST')")
    @Operation(summary = "댓글 개수 조회", description = "노트의 총 댓글 개수를 조회합니다.")
    public ResponseEntity<ApiResponse<Long>> countComments(
            @PathVariable UUID noteId,
            @RequestParam(defaultValue = "false") boolean topLevelOnly
    ) {
        long count = topLevelOnly
                ? commentService.countTopLevelCommentsByNote(noteId)
                : commentService.countCommentsByNote(noteId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
