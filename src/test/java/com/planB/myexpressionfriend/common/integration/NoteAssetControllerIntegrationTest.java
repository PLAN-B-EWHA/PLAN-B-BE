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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

import static org.hamcrest.Matchers.isA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NoteAsset Controller 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("NoteAsset Controller 통합 테스트")
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
        // 사용자와 아동 준비
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

        // 업로드 대상 노트 생성
        note = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("테스트 노트")
                .content("첨부파일 테스트용 노트")
                .isDeleted(false)
                .build();
        noteRepository.save(note);
    }

    @AfterEach
    void tearDown() {
        TestSecurityConfig.clearAuthentication();
    }

    @Test
    @DisplayName("첨부파일 업로드 성공 - 이미지")
    void uploadFile_Image_Success() throws Exception {
        TestSecurityConfig.setAuthentication(therapist);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        mockMvc.perform(multipart("/api/notes/{noteId}/assets", note.getNoteId())
                        .file(file))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.image").value(true));
    }

    @Test
    @DisplayName("권한 없는 사용자는 업로드할 수 없다")
    void uploadFile_AccessDenied() throws Exception {
        TestSecurityConfig.setAuthentication(primaryParent);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "content".getBytes()
        );

        mockMvc.perform(multipart("/api/notes/{noteId}/assets", note.getNoteId())
                        .file(file))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("지원하지 않는 파일 형식은 거부된다")
    void uploadFile_UnsupportedFileType() throws Exception {
        TestSecurityConfig.setAuthentication(therapist);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.exe",
                "application/x-msdownload",
                "content".getBytes()
        );

        mockMvc.perform(multipart("/api/notes/{noteId}/assets", note.getNoteId())
                        .file(file))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("노트별 첨부파일 조회 성공")
    void getAssetsByNote_Success() throws Exception {
        TestSecurityConfig.setAuthentication(therapist);

        mockMvc.perform(get("/api/notes/{noteId}/assets", note.getNoteId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", isA(java.util.List.class)));
    }
}