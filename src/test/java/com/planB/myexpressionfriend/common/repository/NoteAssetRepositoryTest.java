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

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // PostgreSQL 연동 유지
@DisplayName("NoteAssetRepository 테스트")
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
        unauthorizedUser = createUser("unauthorized@test.com", "권한없는사용자", Set.of(UserRole.PARENT));

        // 2. 아동 생성
        child = Child.builder()
                .name("테스트아동")
                .birthDate(LocalDate.of(2020,1,1))
                .gender("MALE")
                .pinEnabled(false)
                .isDeleted(false)
                .build();
        em.persist(child);

        // 3. 권한 설정
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
        note =  ChildNote.builder()
                .child(child)
                .author(primaryParent)
                .type(NoteType.PARENT_NOTE)
                .title("테스트 노트")
                .content("첨부파일 테스트")
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
                5 * 1024L * 1024L, // 5MB
                "video/mp4",
                child.getChildId(),
                note.getNoteId()
        );
        videoAsset.setNote(note);
        em.persist(videoAsset);

        documentAsset = NoteAsset.createAsset(
                "test-document.pdf",
                2 * 1024L * 1024L, // 2MB
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

    // ============= 기본 조회 테스트 =============

    @Test
    @DisplayName("노트의 모든 첨부파일을 조회할 수 있다")
    void findByNoteId_Success() {
        // when
        List<NoteAsset> result = assetRepository.findByNoteId(note.getNoteId());

        // then
        assertThat(result).hasSize(3);
        assertThat(result).extracting("type")
                .containsExactlyInAnyOrder(
                        AssetType.IMAGE,
                        AssetType.VIDEO,
                        AssetType.DOCUMENT
                );
    }

    @Test
    @DisplayName("권한이 있으면 첨부파일을 조회할 수 있다")
    void findByIdWithAuth_WithPermission_Success() {
        //when
        Optional<NoteAsset> result = assetRepository.findByIdWithAuth(
                imageAsset.getAssetId(),
                primaryParent.getUserId()
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getOriginalFileName()).isEqualTo("test-image.jpg");
    }

    @Test
    @DisplayName("권한이 없으면 첨부파일을 조회할 수 없다")
    void findByIdWithAuth_NoPermission_Empty() {
        // when
        Optional<NoteAsset> result = assetRepository.findByIdWithAuth(
                imageAsset.getAssetId(),
                unauthorizedUser.getUserId()
        );

        // then
        assertThat(result).isEmpty();
    }

    // ============= 타입별 조회 테스트 =============

    @Test
    @DisplayName("특정 타입의 첨부파일만 조회할 수 있다")
    void findByNoteIdAndType_Success() {
        // when
        List<NoteAsset> images = assetRepository.findByNoteIdAndType(
                note.getNoteId(),
                AssetType.IMAGE
        );
        List<NoteAsset> videos = assetRepository.findByNoteIdAndType(
                note.getNoteId(),
                AssetType.VIDEO
        );
        // then
        assertThat(images).hasSize(1);
        assertThat(images.get(0).getType()).isEqualTo(AssetType.IMAGE);

        assertThat(videos).hasSize(1);
        assertThat(videos.get(0).getType()).isEqualTo(AssetType.VIDEO);
    }

    @Test
    @DisplayName("아동의 모든 이미지 파일을 조회할 수 있다")
    void findByChildIdWithAuth_Success() {
        // when
        List<NoteAsset> result = assetRepository.findImagesByChildIdWithAuth(
                child.getChildId(),
                primaryParent.getUserId()
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(AssetType.IMAGE);
    }

    @Test
    @DisplayName("권한이 없으면 이미지를 조회할 수 없다")
    void findImagesByChildIdWithAuth_NoPermission_Empty() {
        // when
        List<NoteAsset> result =  assetRepository.findImagesByChildIdWithAuth(
                child.getChildId(),
                unauthorizedUser.getUserId()
        );

        // then
        assertThat(result).isEmpty();
    }

    // ============= 통계 테스트 =============

    @Test
    @DisplayName("노트의 첨부파일 개수를 조회할 수 있다")
    void countByNoteId_Success() {
        // when
        long count = assetRepository.countByNoteId(note.getNoteId());

        // then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("노트의 총 파일 크기를 계산할 수 있다")
    void sumFileSizeByNoteId_Success() {
        // when
        long totalSize = assetRepository.sumFileSizeByNoteId(note.getNoteId());

        // then
        long expectedSize = 1 * 1024 * 1024 + 5 * 1024 * 1024 + 2 * 1024 * 1024; // 8MB
        assertThat(totalSize).isEqualTo(expectedSize);
    }

    @Test
    @DisplayName("아동의 총 파일 크기를 계산할 수 있다")
    void sumFileSizeByChildIdWithAuth_Success() {
        // when
        long totalSize = assetRepository.sumFileSizeByChildIdWithAuth(
                child.getChildId(),
                primaryParent.getUserId()
        );

        // then
        long expectedSize = 1 * 1024 * 1024 + 5 * 1024 * 1024 + 2 * 1024 * 1024; // 8MB
        assertThat(totalSize).isEqualTo(expectedSize);
    }

    @Test
    @DisplayName("권한이 없으면 파일 크기가 0이다")
    void sumFileSizeByChildIdWithAuth_NoPermission_Zero() {
        // when
        long totalSize = assetRepository.sumFileSizeByChildIdWithAuth(
                child.getChildId(),
                unauthorizedUser.getUserId()
        );

        // then
        assertThat(totalSize).isZero();
    }

    // ============= 파일 경로 검증 테스트 =============

    @Test
    @DisplayName("파일 경로가 상대 경로로 저장된다")
    void createAsset_RelativePath() {
        // then
        assertThat(imageAsset.getFileUrl())
                .startsWith("notes/")
                .contains(child.getChildId().toString())
                .contains(note.getNoteId().toString())
                .endsWith(".jpg");
    }

    @Test
    @DisplayName("파일명이 UUID로 난독화된다")
    void createAsset_UuidFileName() {
        // then
        assertThat(imageAsset.getStoredFileName())
                .isNotEqualTo("test-image.jpg")
                .endsWith(".jpg");

        // UUID 형식 검증
        String fileNameWithoutExt = imageAsset.getStoredFileName()
                .substring(0,imageAsset.getStoredFileName().lastIndexOf('.'));
        assertThatCode(() -> UUID.fromString(fileNameWithoutExt))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("원본 파일명은 별도로 저장된다")
    void createAsset_OriginalFileName() {
        // then
        assertThat(imageAsset.getOriginalFileName()).isEqualTo("test-image.jpg");
        assertThat(videoAsset.getOriginalFileName()).isEqualTo("test-video.mp4");
        assertThat(documentAsset.getOriginalFileName()).isEqualTo("test-document.pdf");
    }
}
