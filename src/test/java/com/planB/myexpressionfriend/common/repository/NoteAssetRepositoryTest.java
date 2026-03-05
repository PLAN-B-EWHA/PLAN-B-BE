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
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // PostgreSQL ?곕룞 ?좎?
@DisplayName("NoteAssetRepository ?뚯뒪??)
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
        // 1. ?ъ슜???앹꽦
        primaryParent = createUser("primary@test.com", "二쇰낫?몄옄", Set.of(UserRole.PARENT));
        unauthorizedUser = createUser("unauthorized@test.com", "沅뚰븳?녿뒗?ъ슜??, Set.of(UserRole.PARENT));

        // 2. ?꾨룞 ?앹꽦
        child = Child.builder()
                .name("?뚯뒪?몄븘??)
                .birthDate(LocalDate.of(2020,1,1))
                .gender("MALE")
                .pinEnabled(false)
                .isDeleted(false)
                .build();
        em.persist(child);

        // 3. 沅뚰븳 ?ㅼ젙
        ChildrenAuthorizedUser primaryAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(primaryParent)
                .isPrimary(true)
                .isActive(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .build();
        child.addAuthorizedUser(primaryAuth);
        em.persist(primaryAuth);

        // 4. ?명듃 ?앹꽦
        note =  ChildNote.builder()
                .child(child)
                .author(primaryParent)
                .type(NoteType.PARENT_NOTE)
                .title("?뚯뒪???명듃")
                .content("泥⑤??뚯씪 ?뚯뒪??)
                .isDeleted(false)
                .build();
        em.persist(note);

        // 5. 泥⑤??뚯씪 ?앹꽦
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

    // ============= 湲곕낯 議고쉶 ?뚯뒪??=============

    @Test
    @DisplayName("?명듃??紐⑤뱺 泥⑤??뚯씪??議고쉶?????덈떎")
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
    @DisplayName("沅뚰븳???덉쑝硫?泥⑤??뚯씪??議고쉶?????덈떎")
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
    @DisplayName("沅뚰븳???놁쑝硫?泥⑤??뚯씪??議고쉶?????녿떎")
    void findByIdWithAuth_NoPermission_Empty() {
        // when
        Optional<NoteAsset> result = assetRepository.findByIdWithAuth(
                imageAsset.getAssetId(),
                unauthorizedUser.getUserId()
        );

        // then
        assertThat(result).isEmpty();
    }

    // ============= ??낅퀎 議고쉶 ?뚯뒪??=============

    @Test
    @DisplayName("?뱀젙 ??낆쓽 泥⑤??뚯씪留?議고쉶?????덈떎")
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
    @DisplayName("?꾨룞??紐⑤뱺 이미지 ?뚯씪??議고쉶?????덈떎")
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
    @DisplayName("沅뚰븳???놁쑝硫?이미지瑜?議고쉶?????녿떎")
    void findImagesByChildIdWithAuth_NoPermission_Empty() {
        // when
        List<NoteAsset> result =  assetRepository.findImagesByChildIdWithAuth(
                child.getChildId(),
                unauthorizedUser.getUserId()
        );

        // then
        assertThat(result).isEmpty();
    }

    // ============= ?듦퀎 ?뚯뒪??=============

    @Test
    @DisplayName("?명듃??泥⑤??뚯씪 媛쒖닔瑜?議고쉶?????덈떎")
    void countByNoteId_Success() {
        // when
        long count = assetRepository.countByNoteId(note.getNoteId());

        // then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("?명듃??珥??뚯씪 ?ш린瑜?怨꾩궛?????덈떎")
    void sumFileSizeByNoteId_Success() {
        // when
        long totalSize = assetRepository.sumFileSizeByNoteId(note.getNoteId());

        // then
        long expectedSize = 1 * 1024 * 1024 + 5 * 1024 * 1024 + 2 * 1024 * 1024; // 8MB
        assertThat(totalSize).isEqualTo(expectedSize);
    }

    @Test
    @DisplayName("?꾨룞??珥??뚯씪 ?ш린瑜?怨꾩궛?????덈떎")
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
    @DisplayName("沅뚰븳???놁쑝硫??뚯씪 ?ш린媛 0?대떎")
    void sumFileSizeByChildIdWithAuth_NoPermission_Zero() {
        // when
        long totalSize = assetRepository.sumFileSizeByChildIdWithAuth(
                child.getChildId(),
                unauthorizedUser.getUserId()
        );

        // then
        assertThat(totalSize).isZero();
    }

    // ============= ?뚯씪 寃쎈줈 寃利??뚯뒪??=============

    @Test
    @DisplayName("?뚯씪 寃쎈줈媛 ?곷? 寃쎈줈濡???λ맂??)
    void createAsset_RelativePath() {
        // then
        assertThat(imageAsset.getFileUrl())
                .startsWith("notes/")
                .contains(child.getChildId().toString())
                .contains(note.getNoteId().toString())
                .endsWith(".jpg");
    }

    @Test
    @DisplayName("?뚯씪紐낆씠 UUID濡??쒕룆?붾맂??)
    void createAsset_UuidFileName() {
        // then
        assertThat(imageAsset.getStoredFileName())
                .isNotEqualTo("test-image.jpg")
                .endsWith(".jpg");

        // UUID ?뺤떇 寃利?
        String fileNameWithoutExt = imageAsset.getStoredFileName()
                .substring(0,imageAsset.getStoredFileName().lastIndexOf('.'));
        assertThatCode(() -> UUID.fromString(fileNameWithoutExt))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("?먮낯 ?뚯씪紐낆? 蹂꾨룄濡???λ맂??)
    void createAsset_OriginalFileName() {
        // then
        assertThat(imageAsset.getOriginalFileName()).isEqualTo("test-image.jpg");
        assertThat(videoAsset.getOriginalFileName()).isEqualTo("test-video.mp4");
        assertThat(documentAsset.getOriginalFileName()).isEqualTo("test-document.pdf");
    }
}
