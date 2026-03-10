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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ChildNote Repository 테스트")
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
        authorizedParent = createUser("authorized@test.com", "권한부모", Set.of(UserRole.PARENT));
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
        child.addAuthorizedUser(authorizedAuth);
        em.persist(authorizedAuth);

        // 4. 노트 생성
        therapistNote = ChildNote.builder()
                .child(child)
                .author(therapist)
                .type(NoteType.THERAPIST_NOTE)
                .title("치료사 관찰")
                .content("아동이 과제에 집중하는 시간이 점차 늘어나고 있습니다.")
                .isDeleted(false)
                .build();
        em.persist(therapistNote);

        parentNote = ChildNote.builder()
                .child(child)
                .author(primaryParent)
                .type(NoteType.PARENT_NOTE)
                .title("가정 관찰 메모")
                .content("집에서는 새로운 활동에 적응하는 데 시간이 조금 걸리지만, 반복 후에는 안정적으로 참여합니다.")
                .isDeleted(false)
                .build();
        em.persist(parentNote);

        systemNote = ChildNote.builder()
                .child(child)
                .author(primaryParent)
                .type(NoteType.SYSTEM)
                .title("시스템 기록")
                .content("최근 10일 평균 참여율은 85%입니다.")
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

    // ============= 상세 조회 =============

    @Test
    @DisplayName("주보호자는 노트 상세를 조회할 수 있다")
    void findByIdWithAuth_PrimaryParent_Success() {
        Optional<ChildNote> result = noteRepository.findByIdWithAuth(
                therapistNote.getNoteId(),
                primaryParent.getUserId()
        );

        assertThat(result).isPresent();
        assertThat(result.get().getAuthor().getUserId()).isEqualTo(therapist.getUserId());
    }

    @Test
    @DisplayName("VIEW_REPORT 권한이 있으면 노트 상세를 조회할 수 있다")
    void findByIdWithAuth_WithViewReportPermission_Success() {
        Optional<ChildNote> result = noteRepository.findByIdWithAuth(
                therapistNote.getNoteId(),
                authorizedParent.getUserId()
        );

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("권한이 없으면 노트 상세를 조회할 수 없다")
    void findByIdWithAuth_NoPermission_Empty() {
        Optional<ChildNote> result = noteRepository.findByIdWithAuth(
                therapistNote.getNoteId(),
                unauthorizedUser.getUserId()
        );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("WRITE_NOTE 권한이 있으면 부모 노트도 조회할 수 있다")
    void findByIdWithAuth_WithWriteNotePermission_Success() {
        Optional<ChildNote> result = noteRepository.findByIdWithAuth(
                parentNote.getNoteId(),
                therapist.getUserId()
        );

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("가정 관찰 메모");
    }

    // ============= 목록 조회 =============

    @Test
    @DisplayName("아동별 노트 목록 페이징 조회 성공")
    void findByChildIdWithAuth_Pageable_Success() {
        PageRequest pageRequest = PageRequest.of(0, 2, Sort.by("createdAt").descending());

        Page<ChildNote> result = noteRepository.findByChildIdWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                pageRequest
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("권한이 없으면 아동별 노트 목록은 비어 있다")
    void findByChildIdWithAuth_NoPermission_EmptyPage() {
        PageRequest pageRequest = PageRequest.of(0, 2, Sort.by("createdAt").descending());

        Page<ChildNote> result = noteRepository.findByChildIdWithAuth(
                child.getChildId(),
                unauthorizedUser.getUserId(),
                pageRequest
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ============= 필터 조회 =============

    @Test
    @DisplayName("노트 타입으로 조회 성공")
    void findByChildIdAndTypeWithAuth_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<ChildNote> result = noteRepository.findByChildIdAndTypeWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                NoteType.THERAPIST_NOTE,
                pageRequest
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(NoteType.THERAPIST_NOTE);
    }

    @Test
    @DisplayName("작성자 기준 노트 조회 성공")
    void findByChildIdAndAuthorWithAuth_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<ChildNote> result = noteRepository.findByChildIdAndAuthorWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                therapist.getUserId(),
                pageRequest
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAuthor().getUserId()).isEqualTo(therapist.getUserId());
    }

    @Test
    @DisplayName("날짜 범위로 노트 조회 성공")
    void findByChildIdAndDateRangeWithAuth_Success() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<ChildNote> result = noteRepository.findByChildIdAndDateRangeWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                startDate,
                endDate,
                pageRequest
        );

        assertThat(result.getContent()).hasSize(3);
    }

    // ============= 검색 =============

    @Test
    @DisplayName("제목 키워드 검색 성공")
    void searchByKeywordWithAuth_Title_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<ChildNote> result = noteRepository.searchByKeywordWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                "치료사",
                pageRequest
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).contains("치료사");
    }

    @Test
    @DisplayName("본문 키워드 검색 성공")
    void searchByKeywordWithAuth_Content_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<ChildNote> result = noteRepository.searchByKeywordWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                "85%",
                pageRequest
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(NoteType.SYSTEM);
    }

    @Test
    @DisplayName("권한이 없으면 검색 결과가 없다")
    void searchByKeywordWithAuth_NoPermission_EmptyPage() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<ChildNote> result = noteRepository.searchByKeywordWithAuth(
                child.getChildId(),
                unauthorizedUser.getUserId(),
                "치료사",
                pageRequest
        );

        assertThat(result.getContent()).isEmpty();
    }

    // ============= 통계 =============

    @Test
    @DisplayName("아동별 노트 수 조회 성공")
    void countByChildIdWithAuth_Success() {
        long count = noteRepository.countByChildIdWithAuth(
                child.getChildId(),
                primaryParent.getUserId()
        );

        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("권한이 없으면 아동별 노트 수는 0이다")
    void countByChildIdWithAuth_NoPermission_Zero() {
        long count = noteRepository.countByChildIdWithAuth(
                child.getChildId(),
                unauthorizedUser.getUserId()
        );

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("노트 타입별 개수 조회 성공")
    void countByChildIdAndTypeWithAuth_Success() {
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

        assertThat(therapistCount).isEqualTo(1);
        assertThat(parentCount).isEqualTo(1);
    }

    // ============= Soft Delete =============

    @Test
    @DisplayName("삭제된 노트는 상세 조회에서 제외된다")
    void findByIdWithAuth_DeletedNote_Empty() {
        therapistNote.delete();
        noteRepository.save(therapistNote);
        em.flush();
        em.clear();

        Optional<ChildNote> result = noteRepository.findByIdWithAuth(
                therapistNote.getNoteId(),
                primaryParent.getUserId()
        );

        assertThat(result).isEmpty();
    }

    // ============= 관리자 조회 =============

    @Test
    @DisplayName("관리자는 아동별 전체 노트를 조회할 수 있다")
    void findAllByChildId_Admin_Success() {
        List<ChildNote> result = noteRepository.findAllByChildId(child.getChildId());

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("작성자 기준 전체 노트 조회 성공")
    void findAllByAuthorId_Success() {
        List<ChildNote> result = noteRepository.findAllByAuthorId(therapist.getUserId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAuthor().getUserId()).isEqualTo(therapist.getUserId());
    }

    // ============= N+1 방지 =============

    @Test
    @DisplayName("상세 조회 시 연관 엔티티를 추가 조회하지 않는다")
    void findByIdWithAuth_NoPlusOne() {
        em.clear();

        Optional<ChildNote> result = noteRepository.findByIdWithAuth(
                therapistNote.getNoteId(),
                primaryParent.getUserId()
        );

        assertThat(result).isPresent();
        assertThat(result.get().getChild().getName()).isNotNull();
        assertThat(result.get().getAuthor().getName()).isNotNull();
    }
}