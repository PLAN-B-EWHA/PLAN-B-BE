package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.child.ChildrenAuthorizedUser;
import com.planB.myexpressionfriend.common.domain.note.ChildNote;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // PostgreSQL ?곕룞 ?좎?
@DisplayName("ChildNoteRepository ?뚯뒪??)
@Transactional
public class ChildNoteRepositoryTest {

    @Autowired
    private ChildNoteRepository noteRepository;

    @Autowired
    private TestEntityManager em;

    private User primaryParent;
    private User therapist;
    private User authorizedParent;
    private User unauthorizedUser;
    private Child child;
    private ChildNote therapistNote;
    private ChildNote parentNote;
    private ChildNote systemNote;

    @BeforeEach
    void setUp() {

        // 1. ?ъ슜???앹꽦
        primaryParent = createUser("primary@test.com", "二쇰낫?몄옄", Set.of(UserRole.PARENT));
        therapist = createUser("therapist@test.com", "移섎즺??, Set.of(UserRole.THERAPIST));
        authorizedParent = createUser("authorized@test.com", "沅뚰븳?덈뒗遺紐?, Set.of(UserRole.PARENT));
        unauthorizedUser = createUser("unauthorized@test.com", "沅뚰븳?녿뒗?ъ슜??, Set.of(UserRole.PARENT));

        // 2. ?꾨룞 ?앹꽦
        child = Child.builder()
                .name("?뚯뒪???꾨룞")
                .birthDate(LocalDate.of(2020,1,1))
                .gender("MALE")
                .pinEnabled(false)
                .isDeleted(false)
                .build();
        em.persist(child);

        // 3. 沅뚰븳 ?ㅼ젙
        // 二쇰낫?몄옄
        ChildrenAuthorizedUser primaryAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(primaryParent)
                .isPrimary(true)
                .isActive(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .build();
        child.addAuthorizedUser(primaryAuth);
        em.persist(primaryAuth);

        // 移섎즺??(WRITE_NOTE + VIEW_REPORT)
        ChildrenAuthorizedUser therapistAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .isActive(true)
                .permissions(Set.of(ChildPermissionType.WRITE_NOTE, ChildPermissionType.VIEW_REPORT))
                .build();
        child.addAuthorizedUser(therapistAuth);
        em.persist(therapistAuth);

        ChildrenAuthorizedUser authorizedAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(authorizedParent)
                .isPrimary(false)
                .isActive(true)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .build();
        child.addAuthorizedUser(authorizedAuth); // ?묐갑???곌?愿怨??몄쓽 硫붿꽌???쒖슜
        em.persist(authorizedAuth);

        // 4. ?명듃 ?앹꽦
        therapistNote = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("치료 노트")
                .content("?꾨룞??표정 인식 ?λ젰???μ긽?섍퀬 ?덉뒿?덈떎.")
                .isDeleted(false)
                .build();
        em.persist(therapistNote);

        parentNote = ChildNote.builder()
                .child(child)
                .author(primaryParent)
                .type(NoteType.PARENT_NOTE)
                .title("관찰 일지")
                .content("?ㅻ뒛 ?꾩씠媛 泥섏쓬?쇰줈  誘몄냼瑜?吏?덉뒿?덈떎.")
                .isDeleted(false)
                .build();
        em.persist(parentNote);

        systemNote = ChildNote.builder()
                .child(child)
                .author(primaryParent)
                .type(NoteType.SYSTEM)
                .title("시스템 기록")
                .content("寃뚯엫 10??완료, ?뺥솗??85%")
                .isDeleted(false)
                .build();
        em.persist(systemNote);

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

    // ============= 沅뚰븳 寃利??뚯뒪??=============

    @Test
    @DisplayName("二쇰낫?몄옄???명듃瑜?議고쉶?????덈떎")
    void findByIdWithAuth_PrimaryParent_Success() {
        // when
        Optional<ChildNote> result = noteRepository.findByIdWithAuth(
                therapistNote.getNoteId(),
                primaryParent.getUserId()
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("치료 노트");
    }

    @Test
    @DisplayName("VIEW_REPORT 沅뚰븳???덉쑝硫??명듃瑜?議고쉶?????덈떎")
    void findByIdWithAuth_WithViewReportPermission_Success() {
        // when
        Optional<ChildNote> result = noteRepository.findByIdWithAuth(
                therapistNote.getNoteId(),
                authorizedParent.getUserId()
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("치료 노트");
    }

    @Test
    @DisplayName("沅뚰븳???놁쑝硫??명듃瑜?議고쉶?????녿떎")
    void findByIdWithAuth_NoPermission_Empty() {
        // when
        Optional<ChildNote> result = noteRepository.findByIdWithAuth(
                therapistNote.getNoteId(),
                unauthorizedUser.getUserId()
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("移섎즺?щ뒗 WRITE_NOTE 沅뚰븳?쇰줈 ?명듃瑜?議고쉶?????덈떎")
    void findByIdWithAuth_WithWriteNotePermission_Success() {
        // when
        Optional<ChildNote> result = noteRepository.findByIdWithAuth(
                parentNote.getNoteId(),
                therapist.getUserId()
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("관찰 일지");
    }

    // ============= 紐⑸줉 議고쉶 ?뚯뒪??=============

    @Test
    @DisplayName("?꾨룞??紐⑤뱺 ?명듃瑜??섏씠吏뺥븯??議고쉶?????덈떎")
    void findByChildIdWithAuth_Pageable_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 2, Sort.by("createdAt").descending());

        // when
        Page<ChildNote> result = noteRepository.findByChildIdWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("沅뚰븳???놁쑝硫?鍮??섏씠吏媛 諛섑솚?쒕떎")
    void findByChildIdWithAuth_NoPermission_EmptyPage() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 2, Sort.by("createdAt").descending());

        // when
        Page<ChildNote> result = noteRepository.findByChildIdWithAuth(
                child.getChildId(),
                unauthorizedUser.getUserId(),
                pageRequest
        );

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ============= ?꾪꽣留??뚯뒪??=============

    @Test
    @DisplayName("?명듃 ??낅퀎濡??꾪꽣留곹븯??議고쉶?????덈떎")
    void findByChildIdAndTypeWithAuth_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<ChildNote> result = noteRepository.findByChildIdAndTypeWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                NoteType.THERAPIST_NOTE,
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(NoteType.THERAPIST_NOTE);
    }

    @Test
    @DisplayName("?묒꽦?먮퀎濡??꾪꽣留곹븯??議고쉶?????덈떎")
    void findByChildIdAndAuthorWithAuth_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<ChildNote> result = noteRepository.findByChildIdAndAuthorWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                therapist.getUserId(),
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAuthor().getUserId())
                .isEqualTo(therapist.getUserId());
    }

    @Test
    @DisplayName("?좎쭨 踰붿쐞濡??꾪꽣留곹븯??議고쉶?????덈떎")
    void findByChildIdAndDateRangeWithAuth_Success() {
        // given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<ChildNote> result = noteRepository.findByChildIdAndDateRangeWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                startDate,
                endDate,
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(3);
    }

    // ============= 寃???뚯뒪??=============

    @Test
    @DisplayName("?ㅼ썙?쒕줈 ?명듃瑜?寃?됲븷 ???덈떎 - ?쒕ぉ 寃??)
    void searchByKeywordWithAuth_Title_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<ChildNote> result = noteRepository.searchByKeywordWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                "移섎즺??,
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).contains("移섎즺??);
    }

    @Test
    @DisplayName("?ㅼ썙?쒕줈 ?명듃瑜?寃?됲븷 ???덈떎 - 蹂몃Ц 寃??)
    void searchByKeywordWithAuth_Content_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<ChildNote> result = noteRepository.searchByKeywordWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                "?쒖젙",
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).contains("?쒖젙");
    }

    @Test
    @DisplayName("沅뚰븳???놁쑝硫?寃??寃곌낵媛 ?녿떎")
    void searchByKeywordWithAuth_NoPermission_EmptyPage() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<ChildNote> result = noteRepository.searchByKeywordWithAuth(
                child.getChildId(),
                unauthorizedUser.getUserId(),
                "移섎즺??,
                pageRequest
        );

        // then
        assertThat(result.getContent()).isEmpty();
    }

    // ============= ?듦퀎 ?뚯뒪??=============

    @Test
    @DisplayName("?꾨룞???명듃 珥?媛쒖닔瑜?議고쉶?????덈떎")
    void countByChildIdWithAuth_Success() {
        // when
        long count = noteRepository.countByChildIdWithAuth(
                child.getChildId(),
                primaryParent.getUserId()
        );

        // then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("沅뚰븳???놁쑝硫?媛쒖닔媛 0?대떎")
    void countByChildIdWithAuth_NoPermission_Zero() {
        // when
        long count = noteRepository.countByChildIdWithAuth(
                child.getChildId(),
                unauthorizedUser.getUserId()
        );

        // then
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("?명듃 ??낅퀎 媛쒖닔瑜?議고쉶?????덈떎")
    void countByChildIdAndTypeWithAuth_Success() {
        // when
        long therapistCount = noteRepository.countByChildIdAndTypeWithAuth(
                child.getChildId(),
                therapist.getUserId(),
                NoteType.THERAPIST_NOTE
        );

        long parentCount = noteRepository.countByChildIdAndTypeWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                NoteType.PARENT_NOTE
        );

        // then
        assertThat(therapistCount).isEqualTo(1);
        assertThat(parentCount).isEqualTo(1);
    }

    // ============= Soft Delete ?뚯뒪??=============

    @Test
    @DisplayName("??젣???명듃??議고쉶?섏? ?딅뒗??)
    void findByIdWithAuth_DeletedNote_Empty() {
        // given
        therapistNote.delete();
        noteRepository.save(therapistNote);
        em.flush();
        em.clear();

        // when
        Optional<ChildNote> result = noteRepository.findByIdWithAuth(
                therapistNote.getNoteId(),
                primaryParent.getUserId()
        );

        // then
        assertThat(result).isEmpty();
    }

    // ============= 愿由ъ옄??硫붿꽌???뚯뒪??=============

    @Test
    @DisplayName("愿由ъ옄??沅뚰븳 ?놁씠 紐⑤뱺 ?명듃瑜?議고쉶?????덈떎")
    void findAllByChildId_Admin_Success() {
        // when
        List<ChildNote> result = noteRepository.findAllByChildId(child.getChildId());

        // then
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("?묒꽦?먮퀎濡?紐⑤뱺 ?명듃瑜?議고쉶?????덈떎")
    void findAllByAuthorId_Success() {
        // when
        List<ChildNote> result = noteRepository.findAllByAuthorId(therapist.getUserId());

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAuthor().getUserId()).isEqualTo(therapist.getUserId());
    }

    // ============= N+1 諛⑹? ?뚯뒪??=============

    @Test
    @DisplayName("?명듃 議고쉶 ??N+1 臾몄젣媛 諛쒖깮?섏? ?딅뒗??)
    void findByIdWithAuth_NoPlusOne() {
        // given
        em.clear();

        // when
        Optional<ChildNote> result = noteRepository.findByIdWithAuth(
                therapistNote.getNoteId(),
                primaryParent.getUserId()
        );

        // then
        assertThat(result).isPresent();

        // 吏??濡쒕뵫 ?꾨뱶 ?묎렐 (異붽? 荑쇰━ 諛쒖깮?섏? ?딆븘????
        assertThat(result.get().getChild().getName()).isNotNull();
        assertThat(result.get().getAuthor().getName()).isNotNull();
    }
}
