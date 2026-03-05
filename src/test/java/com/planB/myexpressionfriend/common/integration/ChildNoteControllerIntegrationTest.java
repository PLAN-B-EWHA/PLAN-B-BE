package com.planB.myexpressionfriend.common.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.child.ChildrenAuthorizedUser;
import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.note.NoteType;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import com.planB.myexpressionfriend.common.dto.note.ChildNoteCreateDTO;
import com.planB.myexpressionfriend.common.dto.note.ChildNoteDTO;
import com.planB.myexpressionfriend.common.dto.note.ChildNoteUpdateDTO;
import com.planB.myexpressionfriend.common.repository.ChildNoteRepository;
import com.planB.myexpressionfriend.common.repository.ChildRepository;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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
 * ChildNote Controller ?듯빀 ?뚯뒪??
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)  // Security ?꾪꽣 鍮꾪솢?깊솕
@Transactional
@DisplayName("ChildNote Controller ?듯빀 ?뚯뒪??)
class ChildNoteControllerIntegrationTest {

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

    private User primaryParent;
    private User therapist;
    private User unauthorizedUser;
    private Child child;

    @BeforeEach
    void setUp() {
        // 1. ?ъ슜???앹꽦
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

        unauthorizedUser = User.builder()
                .email("unauthorized@test.com")
                .password("encoded-password")
                .name("沅뚰븳?녿뒗?ъ슜??)
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(unauthorizedUser);

        // 2. ?꾨룞 ?앹꽦
        child = Child.builder()
                .name("?뚯뒪?몄븘??)
                .birthDate(LocalDate.of(2020, 1, 1))
                .gender("MALE")
                .pinEnabled(false)
                .isDeleted(false)
                .build();
        childRepository.save(child);

        // 3. 沅뚰븳 ?ㅼ젙
        ChildrenAuthorizedUser primaryAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(primaryParent)
                .isPrimary(true)
                .isActive(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .build();
        child.addAuthorizedUser(primaryAuth);

        ChildrenAuthorizedUser therapistAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .isActive(true)
                .permissions(Set.of(
                        ChildPermissionType.WRITE_NOTE,
                        ChildPermissionType.VIEW_REPORT
                ))
                .build();
        child.addAuthorizedUser(therapistAuth);

        childRepository.save(child);
    }

    @AfterEach
    void tearDown() {
        TestSecurityConfig.clearAuthentication();
    }

    // ============= ?명듃 ?앹꽦 ?뚯뒪??=============

    @Test
    @DisplayName("?명듃 ?앹꽦 ?깃났 - WRITE_NOTE 沅뚰븳 蹂댁쑀")
    void createNote_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(therapist);

        ChildNoteCreateDTO dto = ChildNoteCreateDTO.builder()
                .childId(child.getChildId())
                .type(NoteType.THERAPIST_NOTE)
                .title("치료 노트")
                .content("?꾨룞??표정 인식 ?λ젰???μ긽?섍퀬 ?덉뒿?덈떎.")
                .build();

        // when & then
        mockMvc.perform(post("/api/children/{childId}/notes", child.getChildId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("노트가 생성되었습니다.))
                .andExpect(jsonPath("$.data.title").value("치료 노트"))
                .andExpect(jsonPath("$.data.type").value("THERAPIST_NOTE"));
    }

    @Test
    @DisplayName("?명듃 ?앹꽦 ?ㅽ뙣 - WRITE_NOTE 沅뚰븳 ?놁쓬")
    void createNote_AccessDenied() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(unauthorizedUser);

        ChildNoteCreateDTO dto = ChildNoteCreateDTO.builder()
                .childId(child.getChildId())
                .type(NoteType.PARENT_NOTE)
                .title("?뚯뒪???명듃")
                .content("沅뚰븳 ?녿뒗 ?ъ슜?먯쓽 ?명듃")
                .build();

        // when & then
        mockMvc.perform(post("/api/children/{childId}/notes", child.getChildId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    // ============= ?명듃 議고쉶 ?뚯뒪??=============
    @Test
    @DisplayName("?명듃 ?곸꽭 議고쉶 ?깃났")
    @WithMockUser(username = "therapist@test.com")
    void getNote_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(therapist);

        ChildNote note = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("치료 노트")
                .content("?곸꽭 ?댁슜?낅땲??")
                .isDeleted(false)
                .build();
        noteRepository.save(note);

        // when & then
        mockMvc.perform(get("/api/notes/{noteId}", note.getNoteId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.noteId").value(note.getNoteId().toString()))
                .andExpect(jsonPath("$.data.title").value("치료 노트"))
                .andExpect(jsonPath("$.data.content").value("?곸꽭 ?댁슜?낅땲??"));
    }

    @Test
    @DisplayName("?명듃 紐⑸줉 議고쉶 ?깃났 - ?섏씠吏?)
    @WithMockUser(username = "primary@test.com")
    void getNotesByChild_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(primaryParent);

        for (int i=1; i<=3; i++) {
            ChildNote note = ChildNote.builder()
                    .child(child)
                    .author(primaryParent)
                    .type(NoteType.PARENT_NOTE)
                    .title("?명듃 "+i)
                    .content("?댁슜 "+i)
                    .isDeleted(false)
                    .build();
            noteRepository.save(note);
        }

        // when & then
        mockMvc.perform(get("/api/children/{childId}/notes", child.getChildId())
                .param("page","0")
                .param("size","2")
                .param("SortBy","createdAt")
                .param("SortOrder","DESC"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));

    }

    @Test
    @DisplayName("?명듃 寃???깃났 - ?ㅼ썙??寃??)
    @WithMockUser(username = "therapist@test.com")
    void searchNotes_ByKeyword_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(therapist);
        TestSecurityConfig.setAuthentication(primaryParent);
        ChildNote note1 = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("표정 인식 ?μ긽")
                .content("표정 인식 ?λ젰??醫뗭븘議뚯뒿?덈떎.")
                .isDeleted(false)
                .build();
        noteRepository.save(note1);

        ChildNote note2 = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("?ы쉶??諛쒕떖")
                .content("?ы쉶?깆씠 ?μ긽?섍퀬 ?덉뒿?덈떎.")
                .isDeleted(false)
                .build();
        noteRepository.save(note2);

        // when & then
        mockMvc.perform(get("/api/children/{childId}/notes/search", child.getChildId())
                        .param("keyword", "?쒖젙"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("표정 인식 ?μ긽"));
    }

    @Test
    @DisplayName("?명듃 寃???깃났 - ????꾪꽣")
    void searchNotes_ByType_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(primaryParent);

        ChildNote parentNote = ChildNote.builder()
                .child(child)
                .author(primaryParent)
                .type(NoteType.PARENT_NOTE)
                .title("遺紐??명듃")
                .content("?댁슜")
                .isDeleted(false)
                .build();
        noteRepository.save(parentNote);

        ChildNote therapistNote = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("移섎즺???명듃")
                .content("?댁슜")
                .isDeleted(false)
                .build();
        noteRepository.save(therapistNote);

        // when & then
        mockMvc.perform(get("/api/children/{childId}/notes/search", child.getChildId())
                        .param("type", "PARENT_NOTE"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].type").value("PARENT_NOTE"));
    }

    // ============= ?명듃 ?섏젙 ?뚯뒪??=============

    @Test
    @DisplayName("?명듃 ?섏젙 ?깃났 - ?묒꽦??蹂몄씤")
    void updateNote_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(therapist);

        ChildNote note = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("?먮낯 ?쒕ぉ")
                .content("?먮낯 ?댁슜")
                .isDeleted(false)
                .build();
        noteRepository.save(note);

        ChildNoteUpdateDTO dto = ChildNoteUpdateDTO.builder()
                .title("?섏젙???쒕ぉ")
                .content("?섏젙???댁슜")
                .build();

        // when & then
        mockMvc.perform(put("/api/notes/{noteId}", note.getNoteId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("?섏젙???쒕ぉ"));
    }

    @Test
    @DisplayName("?명듃 ?섏젙 ?ㅽ뙣 - ?묒꽦???꾨떂")
    void updateNote_AccessDenied() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(primaryParent);

        ChildNote note = ChildNote.builder()
                .child(child)
                .author(therapist)  // 移섎즺?ш? ?묒꽦
                .type(NoteType.THERAPIST_NOTE)
                .title("移섎즺???명듃")
                .content("?댁슜")
                .isDeleted(false)
                .build();
        noteRepository.save(note);

        ChildNoteUpdateDTO dto = ChildNoteUpdateDTO.builder()
                .content("?섏젙 ?쒕룄")
                .build();

        // when & then (二쇰낫?몄옄媛 ?섏젙 ?쒕룄)
        mockMvc.perform(put("/api/notes/{noteId}", note.getNoteId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    // ============= ?명듃 ??젣 ?뚯뒪??=============

    @Test
    @DisplayName("?명듃 ??젣 ?깃났 - ?묒꽦??蹂몄씤")
    void deleteNote_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(therapist);

        ChildNote note = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("??젣???명듃")
                .content("?댁슜")
                .isDeleted(false)
                .build();
        noteRepository.save(note);

        // when & then
        mockMvc.perform(delete("/api/notes/{noteId}", note.getNoteId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("노트가 삭제되었습니다.));
    }

    @Test
    @DisplayName("?명듃 ??젣 ?깃났 - 二쇰낫?몄옄")
    void deleteNote_ByPrimaryParent_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(primaryParent);

        ChildNote note = ChildNote.builder()
                .child(child)
                .author(therapist)  // 移섎즺?ш? ?묒꽦
                .type(NoteType.THERAPIST_NOTE)
                .title("??젣???명듃")
                .content("?댁슜")
                .isDeleted(false)
                .build();
        noteRepository.save(note);

        // when & then (二쇰낫?몄옄媛 ??젣)
        mockMvc.perform(delete("/api/notes/{noteId}", note.getNoteId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }


    // ============= ?듦퀎 ?뚯뒪??=============

    @Test
    @DisplayName("?명듃 媛쒖닔 議고쉶 ?깃났")
    void countNotes_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(primaryParent);

        // 3媛??명듃 ?앹꽦
        for (int i = 0; i < 3; i++) {
            ChildNote note = ChildNote.builder()
                    .child(child)
                    .author(primaryParent)
                    .type(NoteType.PARENT_NOTE)
                    .title("?명듃 " + i)
                    .content("?댁슜")
                    .isDeleted(false)
                    .build();
            noteRepository.save(note);
        }

        //  when & then
        mockMvc.perform(get("/api/children/{childId}/notes/count", child.getChildId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    @DisplayName("?명듃 ??낅퀎 媛쒖닔 議고쉶 ?깃났")
    void countNotesByType_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(primaryParent);

        ChildNote parentNote = ChildNote.builder()
                .child(child)
                .author(primaryParent)
                .type(NoteType.PARENT_NOTE)
                .title("遺紐??명듃")
                .content("?댁슜")
                .isDeleted(false)
                .build();
        noteRepository.save(parentNote);

        ChildNote therapistNote = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("移섎즺???명듃")
                .content("?댁슜")
                .isDeleted(false)
                .build();
        noteRepository.save(therapistNote);

        // when & then
        mockMvc.perform(get("/api/children/{childId}/notes/count", child.getChildId())
                        .param("type", "PARENT_NOTE"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
    }
}