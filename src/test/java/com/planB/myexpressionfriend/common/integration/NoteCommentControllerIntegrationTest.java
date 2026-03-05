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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * NoteComment Controller ?듯빀 ?뚯뒪??
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("NoteComment Controller ?듯빀 ?뚯뒪??)
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
        // ?ъ슜???앹꽦
        primaryParent = User.builder()
                .email("primary@test.com")
                .password("encoded-password")
                .name("二쇰낫?몄옄")
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(primaryParent);

        therapist = User.builder()
                .email("therapist@test.com")
                .password("encoded-password")
                .name("移섎즺??)
                .roles(Set.of(UserRole.THERAPIST))
                .build();
        userRepository.save(therapist);

        // ?꾨룞 ?앹꽦
        child = Child.builder()
                .name("?뚯뒪?몄븘??)
                .birthDate(LocalDate.of(2020, 1, 1))
                .gender("MALE")
                .pinEnabled(false)
                .isDeleted(false)
                .build();
        childRepository.save(child);

        // 沅뚰븳 ?ㅼ젙
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

        // ?명듃 ?앹꽦
        note = ChildNote.builder()
                .child(child)
                .author(primaryParent)
                .type(NoteType.PARENT_NOTE)
                .title("?뚯뒪???명듃")
                .content("?볤? ?뚯뒪??)
                .isDeleted(false)
                .build();
        noteRepository.save(note);
    }

    @AfterEach
    void tearDown() {
        TestSecurityConfig.clearAuthentication();
    }

    // ============= ?볤? ?앹꽦 ?뚯뒪??=============

    @Test
    @DisplayName("댓글 작성 ?깃났 - 理쒖긽???볤?")
    void createComment_TopLevel_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(therapist);

        NoteCommentCreateDTO dto = NoteCommentCreateDTO.builder()
                .noteId(note.getNoteId())
                .content("移섎즺?ъ쓽 ?볤??낅땲??")
                .build();

        // when & then
        mockMvc.perform(post("/api/notes/{noteId}/comments", note.getNoteId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("移섎즺?ъ쓽 ?볤??낅땲??"))
                .andExpect(jsonPath("$.data.topLevel").value(true))
                .andExpect(jsonPath("$.data.authorName").value("移섎즺??));
    }

    @Test
    @DisplayName("댓글 작성 ?깃났 - ??볤?")
    void createComment_Reply_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(primaryParent);

        // 理쒖긽???볤? ?앹꽦
        NoteComment topComment = NoteComment.builder()
                .note(note)
                .author(therapist)
                .content("移섎즺?ъ쓽 ?볤?")
                .isDeleted(false)
                .build();
        commentRepository.save(topComment);

        // ??볤? ?앹꽦
        NoteCommentCreateDTO dto = NoteCommentCreateDTO.builder()
                .noteId(note.getNoteId())
                .parentCommentId(topComment.getCommentId())
                .content("遺紐⑥쓽 ??볤??낅땲??")
                .build();

        // when & then
        mockMvc.perform(post("/api/notes/{noteId}/comments", note.getNoteId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("遺紐⑥쓽 ??볤??낅땲??"))
                .andExpect(jsonPath("$.data.topLevel").value(false))
                .andExpect(jsonPath("$.data.parentCommentId").value(topComment.getCommentId().toString()));
    }

    // ============= ?볤? 議고쉶 ?뚯뒪??=============

    @Test
    @DisplayName("댓글 목록 조회 ?깃났 - 怨꾩링 援ъ“")
    void getCommentsByNote_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(therapist);

        NoteComment topComment = NoteComment.builder()
                .note(note)
                .author(primaryParent)
                .content("遺紐⑥쓽 ?볤?")
                .isDeleted(false)
                .build();
        commentRepository.save(topComment);

        NoteComment reply = NoteComment.builder()
                .note(note)
                .author(therapist)
                .parentComment(topComment)
                .content("移섎즺?ъ쓽 ??볤?")
                .isDeleted(false)
                .build();
        topComment.addReply(reply);
        commentRepository.save(reply);

        // when & then
        mockMvc.perform(get("/api/notes/{noteId}/comments", note.getNoteId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))  // 理쒖긽???볤?留?
                .andExpect(jsonPath("$.data[0].replies", hasSize(1)));  // ??볤? ?ы븿
    }

    // ============= 댓글 수정 ?뚯뒪??=============

    @Test
    @DisplayName("댓글 수정 ?깃났 - ?묒꽦??蹂몄씤")
    void updateComment_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(therapist);

        NoteComment comment = NoteComment.builder()
                .note(note)
                .author(therapist)
                .content("?먮낯 ?볤?")
                .isDeleted(false)
                .build();
        commentRepository.save(comment);

        NoteCommentUpdateDTO dto = NoteCommentUpdateDTO.builder()
                .content("?섏젙???볤?")
                .build();

        // when & then
        mockMvc.perform(put("/api/comments/{commentId}", comment.getCommentId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("?섏젙???볤?"));
    }

    // ============= 댓글 삭제 ?뚯뒪??=============

    @Test
    @DisplayName("댓글 삭제 ?깃났 - 二쇰낫?몄옄")
    void deleteComment_ByPrimaryParent_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(primaryParent);

        NoteComment comment = NoteComment.builder()
                .note(note)
                .author(therapist)
                .content("移섎즺???볤?")
                .isDeleted(false)
                .build();
        commentRepository.save(comment);

        // when & then
        mockMvc.perform(delete("/api/comments/{commentId}", comment.getCommentId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ============= ?듦퀎 ?뚯뒪??=============

    @Test
    @DisplayName("댓글 개수 조회 ?깃났")
    void countComments_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(therapist);

        NoteComment comment1 = NoteComment.builder()
                .note(note)
                .author(therapist)
                .content("?볤? 1")
                .isDeleted(false)
                .build();
        commentRepository.save(comment1);

        NoteComment comment2 = NoteComment.builder()
                .note(note)
                .author(primaryParent)
                .content("?볤? 2")
                .isDeleted(false)
                .build();
        commentRepository.save(comment2);

        // when & then
        mockMvc.perform(get("/api/notes/{noteId}/comments/count", note.getNoteId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));
    }
}
