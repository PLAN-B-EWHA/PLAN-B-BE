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
 * ChildNote Controller 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)  // Security 필터 비활성화
@Transactional
@DisplayName("ChildNote Controller 통합 테스트")
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
        // 1. 사용자 생성
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

        unauthorizedUser = User.builder()
                .email("unauthorized@test.com")
                .password("encoded-password")
                .name("권한없는사용자")
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(unauthorizedUser);

        // 2. 아동 생성
        child = Child.builder()
                .name("테스트아동")
                .birthDate(LocalDate.of(2020, 1, 1))
                .gender("MALE")
                .pinEnabled(false)
                .isDeleted(false)
                .build();
        childRepository.save(child);

        // 3. 권한 설정
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

    // ============= 노트 생성 테스트 =============

    @Test
    @DisplayName("노트 생성 성공 - WRITE_NOTE 권한 보유")
    void createNote_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(therapist);

        ChildNoteCreateDTO dto = ChildNoteCreateDTO.builder()
                .childId(child.getChildId())
                .type(NoteType.THERAPIST_NOTE)
                .title("치료사 소견")
                .content("아동의 표정 인식 능력이 향상되고 있습니다.")
                .build();

        // when & then
        mockMvc.perform(post("/api/children/{childId}/notes", child.getChildId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("노트가 생성되었습니다"))
                .andExpect(jsonPath("$.data.title").value("치료사 소견"))
                .andExpect(jsonPath("$.data.type").value("THERAPIST_NOTE"));
    }

    @Test
    @DisplayName("노트 생성 실패 - WRITE_NOTE 권한 없음")
    void createNote_AccessDenied() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(unauthorizedUser);

        ChildNoteCreateDTO dto = ChildNoteCreateDTO.builder()
                .childId(child.getChildId())
                .type(NoteType.PARENT_NOTE)
                .title("테스트 노트")
                .content("권한 없는 사용자의 노트")
                .build();

        // when & then
        mockMvc.perform(post("/api/children/{childId}/notes", child.getChildId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    // ============= 노트 조회 테스트 =============
    @Test
    @DisplayName("노트 상세 조회 성공")
    @WithMockUser(username = "therapist@test.com")
    void getNote_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(therapist);

        ChildNote note = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("치료사 소견")
                .content("상세 내용입니다.")
                .isDeleted(false)
                .build();
        noteRepository.save(note);

        // when & then
        mockMvc.perform(get("/api/notes/{noteId}", note.getNoteId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.noteId").value(note.getNoteId().toString()))
                .andExpect(jsonPath("$.data.title").value("치료사 소견"))
                .andExpect(jsonPath("$.data.content").value("상세 내용입니다."));
    }

    @Test
    @DisplayName("노트 목록 조회 성공 - 페이징")
    @WithMockUser(username = "primary@test.com")
    void getNotesByChild_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(primaryParent);

        for (int i=1; i<=3; i++) {
            ChildNote note = ChildNote.builder()
                    .child(child)
                    .author(primaryParent)
                    .type(NoteType.PARENT_NOTE)
                    .title("노트 "+i)
                    .content("내용 "+i)
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
    @DisplayName("노트 검색 성공 - 키워드 검색")
    @WithMockUser(username = "therapist@test.com")
    void searchNotes_ByKeyword_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(therapist);
        TestSecurityConfig.setAuthentication(primaryParent);
        ChildNote note1 = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("표정 인식 향상")
                .content("표정 인식 능력이 좋아졌습니다.")
                .isDeleted(false)
                .build();
        noteRepository.save(note1);

        ChildNote note2 = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("사회성 발달")
                .content("사회성이 향상되고 있습니다.")
                .isDeleted(false)
                .build();
        noteRepository.save(note2);

        // when & then
        mockMvc.perform(get("/api/children/{childId}/notes/search", child.getChildId())
                        .param("keyword", "표정"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("표정 인식 향상"));
    }

    @Test
    @DisplayName("노트 검색 성공 - 타입 필터")
    void searchNotes_ByType_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(primaryParent);

        ChildNote parentNote = ChildNote.builder()
                .child(child)
                .author(primaryParent)
                .type(NoteType.PARENT_NOTE)
                .title("부모 노트")
                .content("내용")
                .isDeleted(false)
                .build();
        noteRepository.save(parentNote);

        ChildNote therapistNote = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("치료사 노트")
                .content("내용")
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

    // ============= 노트 수정 테스트 =============

    @Test
    @DisplayName("노트 수정 성공 - 작성자 본인")
    void updateNote_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(therapist);

        ChildNote note = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("원본 제목")
                .content("원본 내용")
                .isDeleted(false)
                .build();
        noteRepository.save(note);

        ChildNoteUpdateDTO dto = ChildNoteUpdateDTO.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .build();

        // when & then
        mockMvc.perform(put("/api/notes/{noteId}", note.getNoteId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("수정된 제목"));
    }

    @Test
    @DisplayName("노트 수정 실패 - 작성자 아님")
    void updateNote_AccessDenied() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(primaryParent);

        ChildNote note = ChildNote.builder()
                .child(child)
                .author(therapist)  // 치료사가 작성
                .type(NoteType.THERAPIST_NOTE)
                .title("치료사 노트")
                .content("내용")
                .isDeleted(false)
                .build();
        noteRepository.save(note);

        ChildNoteUpdateDTO dto = ChildNoteUpdateDTO.builder()
                .content("수정 시도")
                .build();

        // when & then (주보호자가 수정 시도)
        mockMvc.perform(put("/api/notes/{noteId}", note.getNoteId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    // ============= 노트 삭제 테스트 =============

    @Test
    @DisplayName("노트 삭제 성공 - 작성자 본인")
    void deleteNote_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(therapist);

        ChildNote note = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("삭제할 노트")
                .content("내용")
                .isDeleted(false)
                .build();
        noteRepository.save(note);

        // when & then
        mockMvc.perform(delete("/api/notes/{noteId}", note.getNoteId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("노트가 삭제되었습니다"));
    }

    @Test
    @DisplayName("노트 삭제 성공 - 주보호자")
    void deleteNote_ByPrimaryParent_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(primaryParent);

        ChildNote note = ChildNote.builder()
                .child(child)
                .author(therapist)  // 치료사가 작성
                .type(NoteType.THERAPIST_NOTE)
                .title("삭제할 노트")
                .content("내용")
                .isDeleted(false)
                .build();
        noteRepository.save(note);

        // when & then (주보호자가 삭제)
        mockMvc.perform(delete("/api/notes/{noteId}", note.getNoteId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }


    // ============= 통계 테스트 =============

    @Test
    @DisplayName("노트 개수 조회 성공")
    void countNotes_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(primaryParent);

        // 3개 노트 생성
        for (int i = 0; i < 3; i++) {
            ChildNote note = ChildNote.builder()
                    .child(child)
                    .author(primaryParent)
                    .type(NoteType.PARENT_NOTE)
                    .title("노트 " + i)
                    .content("내용")
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
    @DisplayName("노트 타입별 개수 조회 성공")
    void countNotesByType_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(primaryParent);

        ChildNote parentNote = ChildNote.builder()
                .child(child)
                .author(primaryParent)
                .type(NoteType.PARENT_NOTE)
                .title("부모 노트")
                .content("내용")
                .isDeleted(false)
                .build();
        noteRepository.save(parentNote);

        ChildNote therapistNote = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("치료사 노트")
                .content("내용")
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