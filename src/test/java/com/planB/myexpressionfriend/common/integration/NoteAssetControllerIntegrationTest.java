package com.planB.myexpressionfriend.common.integration;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.child.ChildrenAuthorizedUser;
import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.note.NoteType;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
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
import org.springframework.mock.web.MockMultipartFile;
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
 * NoteAsset Controller ?듯빀 ?뚯뒪??
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("NoteAsset Controller ?듯빀 ?뚯뒪??)
public class NoteAssetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChildRepository childRepository;

    @Autowired
    private ChildNoteRepository noteRepository;

    private User primaryParent;
    private User therapist;
    private Child child;
    private ChildNote note;

    @BeforeEach
    void setUp() {
        // ?ъ슜??諛??꾨룞 ?앹꽦
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
                .permissions(Set.of(
                        ChildPermissionType.VIEW_REPORT,
                        ChildPermissionType.WRITE_NOTE
                ))
                .build();
        child.addAuthorizedUser(primaryAuth);

        ChildrenAuthorizedUser therapistAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .isActive(true)
                .permissions(Set.of(
                        ChildPermissionType.VIEW_REPORT,
                        ChildPermissionType.WRITE_NOTE
                ))
                .build();
        child.addAuthorizedUser(therapistAuth);

        childRepository.save(child);

        // ?명듃 ?앹꽦
        note = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("?뚯뒪???명듃")
                .content("?뚯씪 ?낅줈???뚯뒪??)
                .isDeleted(false)
                .build();
        noteRepository.save(note);
    }

    @AfterEach
    void tearDown() {
        TestSecurityConfig.clearAuthentication();
    }

    // ============= ?뚯씪 ?낅줈???뚯뒪??=============

    @Test
    @DisplayName("?뚯씪 ?낅줈???깃났 - 이미지")
    void uploadFile_Image_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(therapist);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // when & then
        mockMvc.perform(multipart("/api/notes/{noteId}/assets", note.getNoteId())
                        .file(file))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.originalFileName").value("test-image.jpg"))
                .andExpect(jsonPath("$.data.type").value("IMAGE"))
                .andExpect(jsonPath("$.data.image").value(true));
    }


    @Test
    @DisplayName("?뚯씪 ?낅줈???ㅽ뙣 - ?묒꽦???꾨떂")
    void uploadFile_AccessDenied() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(primaryParent);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "content".getBytes()
        );

        // when & then (二쇰낫?몄옄媛 移섎즺???명듃???뚯씪 ?낅줈???쒕룄)
        mockMvc.perform(multipart("/api/notes/{noteId}/assets", note.getNoteId())
                        .file(file))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("?뚯씪 ?낅줈???ㅽ뙣 - 吏?먰븯吏 ?딅뒗 ?뚯씪 ???)
    void uploadFile_UnsupportedFileType() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(therapist);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.exe",
                "application/x-msdownload",
                "content".getBytes()
        );

        // when & then
        mockMvc.perform(multipart("/api/notes/{noteId}/assets", note.getNoteId())
                        .file(file))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ============= ?뚯씪 議고쉶 ?뚯뒪??=============

    @Test
    @DisplayName("泥⑤??뚯씪 紐⑸줉 議고쉶 ?깃났")
    void getAssetsByNote_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(therapist);

        // when & then
        mockMvc.perform(get("/api/notes/{noteId}/assets", note.getNoteId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", isA(java.util.List.class)));
    }
}
