package com.planB.myexpressionfriend.common.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.child.ChildrenAuthorizedUser;
import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.note.NoteComment;
import com.planB.myexpressionfriend.common.domain.note.NoteType;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import com.planB.myexpressionfriend.common.dto.note.NoteCommentCreateDTO;
import com.planB.myexpressionfriend.common.dto.note.NoteCommentUpdateDTO;
import com.planB.myexpressionfriend.common.repository.ChildNoteRepository;
import com.planB.myexpressionfriend.common.repository.ChildRepository;
import com.planB.myexpressionfriend.common.repository.NoteCommentRepository;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NoteComment Controller 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("NoteComment Controller 통합 테스트")
public class NoteCommentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChildRepository childRepository;

    @Autowired
    private ChildNoteRepository noteRepository;

    @Autowired
    private NoteCommentRepository commentRepository;

    private User primaryParent;
    private User therapist;
    private Child child;
    private ChildNote note;

    @BeforeEach
    void setUp() {
        // 사용자 생성
        primaryParent = User.builder()
                .email("primary@test.com")
                .password("encoded-password")
                .name("주보호자")
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(primaryParent);

        therapist = User.builder()
                .email("therapist@test.com")
                .password("encoded-password")
                .name("치료사")
                .roles(Set.of(UserRole.THERAPIST))
                .build();
        userRepository.save(therapist);

        // 아동 생성
        child = Child.builder()
                .name("테스트아동")
                .birthDate(LocalDate.of(2020, 1, 1))
                .gender("MALE")
                .pinEnabled(false)
                .isDeleted(false)
                .build();
        childRepository.save(child);

        // 권한 연결
        ChildrenAuthorizedUser primaryAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(primaryParent)
                .isPrimary(true)
                .isActive(true)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .build();
        child.addAuthorizedUser(primaryAuth);

        ChildrenAuthorizedUser therapistAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .isActive(true)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .build();
        child.addAuthorizedUser(therapistAuth);

        childRepository.save(child);

        // 노트 생성
        note = ChildNote.builder()
                .child(child)
                .author(primaryParent)
                .type(NoteType.PARENT_NOTE)
                .title("오늘 관찰 기록")
                .content("오늘 수업 참여도가 좋았습니다.")
                .isDeleted(false)
                .build();
        noteRepository.save(note);
    }

    @AfterEach
    void tearDown() {
        TestSecurityConfig.clearAuthentication();
    }

    @Test
    @DisplayName("댓글 생성 성공 - 최상위 댓글")
    void createComment_TopLevel_Success() throws Exception {
        TestSecurityConfig.setAuthentication(therapist);

        NoteCommentCreateDTO dto = NoteCommentCreateDTO.builder()
                .noteId(note.getNoteId())
                .content("수업 집중도가 이전보다 좋아졌습니다.")
                .build();

        mockMvc.perform(post("/api/notes/{noteId}/comments", note.getNoteId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.topLevel").value(true))
                .andExpect(jsonPath("$.data.authorName").value("치료사"));
    }

    @Test
    @DisplayName("댓글 생성 성공 - 답글")
    void createComment_Reply_Success() throws Exception {
        TestSecurityConfig.setAuthentication(primaryParent);

        NoteComment topComment = NoteComment.builder()
                .note(note)
                .author(therapist)
                .content("수업 집중도가 좋아졌습니다.")
                .isDeleted(false)
                .build();
        commentRepository.save(topComment);

        NoteCommentCreateDTO dto = NoteCommentCreateDTO.builder()
                .noteId(note.getNoteId())
                .parentCommentId(topComment.getCommentId())
                .content("좋은 피드백 감사합니다.")
                .build();

        mockMvc.perform(post("/api/notes/{noteId}/comments", note.getNoteId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topLevel").value(false))
                .andExpect(jsonPath("$.data.parentCommentId").value(topComment.getCommentId().toString()));
    }

    @Test
    @DisplayName("노트별 댓글 조회 성공")
    void getCommentsByNote_Success() throws Exception {
        TestSecurityConfig.setAuthentication(therapist);

        NoteComment topComment = NoteComment.builder()
                .note(note)
                .author(primaryParent)
                .content("추가로 확인 부탁드립니다.")
                .isDeleted(false)
                .build();
        commentRepository.save(topComment);

        NoteComment reply = NoteComment.builder()
                .note(note)
                .author(therapist)
                .parentComment(topComment)
                .content("확인 후 다시 안내드리겠습니다.")
                .isDeleted(false)
                .build();
        topComment.addReply(reply);
        commentRepository.save(reply);

        mockMvc.perform(get("/api/notes/{noteId}/comments", note.getNoteId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].replies", hasSize(1)));
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void updateComment_Success() throws Exception {
        TestSecurityConfig.setAuthentication(therapist);

        NoteComment comment = NoteComment.builder()
                .note(note)
                .author(therapist)
                .content("초기 댓글")
                .isDeleted(false)
                .build();
        commentRepository.save(comment);

        NoteCommentUpdateDTO dto = NoteCommentUpdateDTO.builder()
                .content("수정된 댓글 내용")
                .build();

        mockMvc.perform(put("/api/comments/{commentId}", comment.getCommentId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("수정된 댓글 내용"));
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void deleteComment_ByPrimaryParent_Success() throws Exception {
        TestSecurityConfig.setAuthentication(primaryParent);

        NoteComment comment = NoteComment.builder()
                .note(note)
                .author(therapist)
                .content("삭제할 댓글")
                .isDeleted(false)
                .build();
        commentRepository.save(comment);

        mockMvc.perform(delete("/api/comments/{commentId}", comment.getCommentId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("댓글 수 조회 성공")
    void countComments_Success() throws Exception {
        TestSecurityConfig.setAuthentication(therapist);

        NoteComment comment1 = NoteComment.builder()
                .note(note)
                .author(therapist)
                .content("댓글 1")
                .isDeleted(false)
                .build();
        commentRepository.save(comment1);

        NoteComment comment2 = NoteComment.builder()
                .note(note)
                .author(primaryParent)
                .content("댓글 2")
                .isDeleted(false)
                .build();
        commentRepository.save(comment2);

        mockMvc.perform(get("/api/notes/{noteId}/comments/count", note.getNoteId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));
    }
}