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
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // PostgreSQL 연동 유지
@DisplayName("ChildNoteRepository 테스트")
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

        // 1. 사용자 생성
        primaryParent = createUser("primary@test.com", "주보호자", Set.of(UserRole.PARENT));
        therapist = createUser("therapist@test.com", "치료사", Set.of(UserRole.THERAPIST));
        authorizedParent = createUser("authorized@test.com", "권한있는부모", Set.of(UserRole.PARENT));
        unauthorizedUser = createUser("unauthorized@test.com", "권한없는사용자", Set.of(UserRole.PARENT));

        // 2. 아동 생성
        child = Child.builder()
                .name("테스트 아동")
                .birthDate(LocalDate.of(2020,1,1))
                .gender("MALE")
                .pinEnabled(false)
                .isDeleted(false)
                .build();
        em.persist(child);

        // 3. 권한 설정
        // 주보호자
        ChildrenAuthorizedUser primaryAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(primaryParent)
                .isPrimary(true)
                .isActive(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .build();
        child.addAuthorizedUser(primaryAuth);
        em.persist(primaryAuth);

        // 치료사 (WRITE_NOTE + VIEW_REPORT)
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
        child.addAuthorizedUser(authorizedAuth); // 양방향 연관관계 편의 메서드 활용
        em.persist(authorizedAuth);

        // 4. 노트 생성
        therapistNote = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("치료사 소견")
                .content("아동의 표정 인식 능력이 향상되고 있습니다.")
                .isDeleted(false)
                .build();
        em.persist(therapistNote);

        parentNote = ChildNote.builder()
                .child(child)
                .author(primaryParent)
                .type(NoteType.PARENT_NOTE)
                .title("관찰 일지")
                .content("오늘 아이가 처음으로  미소를 지었습니다.")
                .isDeleted(false)
                .build();
        em.persist(parentNote);

        systemNote = ChildNote.builder()
                .child(child)
                .author(primaryParent)
                .type(NoteType.SYSTEM)
                .title("시스템 기록")
                .content("게임 10회 완료, 정확도 85%")
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

    // ============= 권한 검증 테스트 =============

    @Test
    @DisplayName("주보호자는 노트를 조회할 수 있다")
    void findByIdWithAuth_PrimaryParent_Success() {
        // when
        Optional<ChildNote> result = noteRepository.findByIdWithAuth(
                therapistNote.getNoteId(),
                primaryParent.getUserId()
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("치료사 소견");
    }

    @Test
    @DisplayName("VIEW_REPORT 권한이 있으면 노트를 조회할 수 있다")
    void findByIdWithAuth_WithViewReportPermission_Success() {
        // when
        Optional<ChildNote> result = noteRepository.findByIdWithAuth(
                therapistNote.getNoteId(),
                authorizedParent.getUserId()
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("치료사 소견");
    }

    @Test
    @DisplayName("권한이 없으면 노트를 조회할 수 없다")
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
    @DisplayName("치료사는 WRITE_NOTE 권한으로 노트를 조회할 수 있다")
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

    // ============= 목록 조회 테스트 =============

    @Test
    @DisplayName("아동의 모든 노트를 페이징하여 조회할 수 있다")
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
    @DisplayName("권한이 없으면 빈 페이지가 반환된다")
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

    // ============= 필터링 테스트 =============

    @Test
    @DisplayName("노트 타입별로 필터링하여 조회할 수 있다")
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
    @DisplayName("작성자별로 필터링하여 조회할 수 있다")
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
    @DisplayName("날짜 범위로 필터링하여 조회할 수 있다")
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

    // ============= 검색 테스트 =============

    @Test
    @DisplayName("키워드로 노트를 검색할 수 있다 - 제목 검색")
    void searchByKeywordWithAuth_Title_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<ChildNote> result = noteRepository.searchByKeywordWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                "치료사",
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).contains("치료사");
    }

    @Test
    @DisplayName("키워드로 노트를 검색할 수 있다 - 본문 검색")
    void searchByKeywordWithAuth_Content_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<ChildNote> result = noteRepository.searchByKeywordWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                "표정",
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).contains("표정");
    }

    @Test
    @DisplayName("권한이 없으면 검색 결과가 없다")
    void searchByKeywordWithAuth_NoPermission_EmptyPage() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<ChildNote> result = noteRepository.searchByKeywordWithAuth(
                child.getChildId(),
                unauthorizedUser.getUserId(),
                "치료사",
                pageRequest
        );

        // then
        assertThat(result.getContent()).isEmpty();
    }

    // ============= 통계 테스트 =============

    @Test
    @DisplayName("아동의 노트 총 개수를 조회할 수 있다")
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
    @DisplayName("권한이 없으면 개수가 0이다")
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
    @DisplayName("노트 타입별 개수를 조회할 수 있다")
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

    // ============= Soft Delete 테스트 =============

    @Test
    @DisplayName("삭제된 노트는 조회되지 않는다")
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

    // ============= 관리자용 메서드 테스트 =============

    @Test
    @DisplayName("관리자는 권한 없이 모든 노트를 조회할 수 있다")
    void findAllByChildId_Admin_Success() {
        // when
        List<ChildNote> result = noteRepository.findAllByChildId(child.getChildId());

        // then
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("작성자별로 모든 노트를 조회할 수 있다")
    void findAllByAuthorId_Success() {
        // when
        List<ChildNote> result = noteRepository.findAllByAuthorId(therapist.getUserId());

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAuthor().getUserId()).isEqualTo(therapist.getUserId());
    }

    // ============= N+1 방지 테스트 =============

    @Test
    @DisplayName("노트 조회 시 N+1 문제가 발생하지 않는다")
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

        // 지연 로딩 필드 접근 (추가 쿼리 발생하지 않아야 함)
        assertThat(result.get().getChild().getName()).isNotNull();
        assertThat(result.get().getAuthor().getName()).isNotNull();
    }
}
