package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.child.ChildrenAuthorizedUser;
import com.planB.myexpressionfriend.common.domain.note.AssetType;
import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.note.NoteAsset;
import com.planB.myexpressionfriend.common.domain.note.NoteType;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("NoteAsset Repository 테스트")
@Transactional
public class NoteAssetRepositoryTest {

    @Autowired
    private NoteAssetRepository assetRepository;

    @Autowired
    private TestEntityManager em;

    private User primaryParent;
    private User unauthorizedUser;
    private Child child;
    private ChildNote note;
    private NoteAsset imageAsset;
    private NoteAsset videoAsset;
    private NoteAsset documentAsset;

    @BeforeEach
    void setup() {
        // 1. 사용자 생성
        primaryParent = createUser("primary@test.com", "주보호자", Set.of(UserRole.PARENT));
        unauthorizedUser = createUser("unauthorized@test.com", "권한없음", Set.of(UserRole.PARENT));

        // 2. 아동 생성
        child = Child.builder()
                .name("테스트아동")
                .birthDate(LocalDate.of(2020, 1, 1))
                .gender("MALE")
                .pinEnabled(false)
                .isDeleted(false)
                .build();
        em.persist(child);

        // 3. 권한 연결
        ChildrenAuthorizedUser primaryAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(primaryParent)
                .isPrimary(true)
                .isActive(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .build();
        child.addAuthorizedUser(primaryAuth);
        em.persist(primaryAuth);

        // 4. 노트 생성
        note = ChildNote.builder()
                .child(child)
                .author(primaryParent)
                .type(NoteType.PARENT_NOTE)
                .title("테스트 노트")
                .content("테스트 내용")
                .isDeleted(false)
                .build();
        em.persist(note);

        // 5. 첨부파일 생성
        imageAsset = NoteAsset.createAsset(
                "test-image.jpg",
                1024L * 1024L,
                "image/jpeg",
                child.getChildId(),
                note.getNoteId()
        );
        imageAsset.setNote(note);
        em.persist(imageAsset);

        videoAsset = NoteAsset.createAsset(
                "test-video.mp4",
                5 * 1024L * 1024L,
                "video/mp4",
                child.getChildId(),
                note.getNoteId()
        );
        videoAsset.setNote(note);
        em.persist(videoAsset);

        documentAsset = NoteAsset.createAsset(
                "test-document.pdf",
                2 * 1024L * 1024L,
                "application/pdf",
                child.getChildId(),
                note.getNoteId()
        );
        documentAsset.setNote(note);
        em.persist(documentAsset);

        em.flush();
        em.clear();
    }

    private User createUser(String email, String name, Set<UserRole> roles) {
        User user = User.builder()
                .email(email)
                .password("encoded-password")
                .name(name)
                .roles(roles)
                .build();
        em.persist(user);
        return user;
    }

    // ============= 기본 조회 =============

    @Test
    @DisplayName("노트 ID로 첨부파일 목록 조회")
    void findByNoteId_Success() {
        List<NoteAsset> result = assetRepository.findByNoteId(note.getNoteId());

        assertThat(result).hasSize(3);
        assertThat(result).extracting("type")
                .containsExactlyInAnyOrder(AssetType.IMAGE, AssetType.VIDEO, AssetType.DOCUMENT);
    }

    @Test
    @DisplayName("권한이 있으면 첨부파일 상세 조회 성공")
    void findByIdWithAuth_WithPermission_Success() {
        Optional<NoteAsset> result = assetRepository.findByIdWithAuth(
                imageAsset.getAssetId(),
                primaryParent.getUserId()
        );

        assertThat(result).isPresent();
        assertThat(result.get().getOriginalFileName()).isEqualTo("test-image.jpg");
    }

    @Test
    @DisplayName("권한이 없으면 첨부파일 상세 조회 실패")
    void findByIdWithAuth_NoPermission_Empty() {
        Optional<NoteAsset> result = assetRepository.findByIdWithAuth(
                imageAsset.getAssetId(),
                unauthorizedUser.getUserId()
        );

        assertThat(result).isEmpty();
    }

    // ============= 타입별 조회 =============

    @Test
    @DisplayName("노트 ID와 타입으로 첨부파일 조회")
    void findByNoteIdAndType_Success() {
        List<NoteAsset> images = assetRepository.findByNoteIdAndType(note.getNoteId(), AssetType.IMAGE);
        List<NoteAsset> videos = assetRepository.findByNoteIdAndType(note.getNoteId(), AssetType.VIDEO);

        assertThat(images).hasSize(1);
        assertThat(images.get(0).getType()).isEqualTo(AssetType.IMAGE);
        assertThat(videos).hasSize(1);
        assertThat(videos.get(0).getType()).isEqualTo(AssetType.VIDEO);
    }

    @Test
    @DisplayName("권한이 있으면 아동별 이미지 조회 성공")
    void findByChildIdWithAuth_Success() {
        List<NoteAsset> result = assetRepository.findImagesByChildIdWithAuth(
                child.getChildId(),
                primaryParent.getUserId()
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(AssetType.IMAGE);
    }

    @Test
    @DisplayName("권한이 없으면 아동별 이미지 조회 실패")
    void findImagesByChildIdWithAuth_NoPermission_Empty() {
        List<NoteAsset> result = assetRepository.findImagesByChildIdWithAuth(
                child.getChildId(),
                unauthorizedUser.getUserId()
        );

        assertThat(result).isEmpty();
    }

    // ============= 통계 =============

    @Test
    @DisplayName("노트별 첨부파일 개수 조회")
    void countByNoteId_Success() {
        long count = assetRepository.countByNoteId(note.getNoteId());

        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("노트별 전체 첨부파일 용량 합계 조회")
    void sumFileSizeByNoteId_Success() {
        long totalSize = assetRepository.sumFileSizeByNoteId(note.getNoteId());

        long expectedSize = 1 * 1024 * 1024 + 5 * 1024 * 1024 + 2 * 1024 * 1024;
        assertThat(totalSize).isEqualTo(expectedSize);
    }

    @Test
    @DisplayName("권한이 있으면 아동별 전체 첨부파일 용량 조회 성공")
    void sumFileSizeByChildIdWithAuth_Success() {
        long totalSize = assetRepository.sumFileSizeByChildIdWithAuth(
                child.getChildId(),
                primaryParent.getUserId()
        );

        long expectedSize = 1 * 1024 * 1024 + 5 * 1024 * 1024 + 2 * 1024 * 1024;
        assertThat(totalSize).isEqualTo(expectedSize);
    }

    @Test
    @DisplayName("권한이 없으면 아동별 전체 첨부파일 용량은 0이다")
    void sumFileSizeByChildIdWithAuth_NoPermission_Zero() {
        long totalSize = assetRepository.sumFileSizeByChildIdWithAuth(
                child.getChildId(),
                unauthorizedUser.getUserId()
        );

        assertThat(totalSize).isZero();
    }

    // ============= 파일명 생성 =============

    @Test
    @DisplayName("저장 경로는 상대 경로로 생성된다")
    void createAsset_RelativePath() {
        assertThat(imageAsset.getFileUrl())
                .startsWith("notes/")
                .contains(child.getChildId().toString())
                .contains(note.getNoteId().toString())
                .endsWith(".jpg");
    }

    @Test
    @DisplayName("저장 파일명은 UUID 기반으로 생성된다")
    void createAsset_UuidFileName() {
        assertThat(imageAsset.getStoredFileName())
                .isNotEqualTo("test-image.jpg")
                .endsWith(".jpg");

        String fileNameWithoutExt = imageAsset.getStoredFileName()
                .substring(0, imageAsset.getStoredFileName().lastIndexOf('.'));
        assertThatCode(() -> UUID.fromString(fileNameWithoutExt))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("원본 파일명은 그대로 보존된다")
    void createAsset_OriginalFileName() {
        assertThat(imageAsset.getOriginalFileName()).isEqualTo("test-image.jpg");
        assertThat(videoAsset.getOriginalFileName()).isEqualTo("test-video.mp4");
        assertThat(documentAsset.getOriginalFileName()).isEqualTo("test-document.pdf");
    }
}