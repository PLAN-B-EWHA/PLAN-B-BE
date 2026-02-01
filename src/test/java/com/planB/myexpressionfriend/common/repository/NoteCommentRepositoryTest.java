package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.child.ChildrenAuthorizedUser;
import com.planB.myexpressionfriend.common.domain.note.ChildNote;
import com.planB.myexpressionfriend.common.domain.note.NoteComment;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // PostgreSQL 연동 유지
@DisplayName("NoteCommentRepository 테스트")
@Transactional
public class NoteCommentRepositoryTest {

    @Autowired
    private NoteCommentRepository commentRepository;

    @Autowired
    private TestEntityManager em;

    private User primaryParent;
    private User therapist;
    private User unauthorizedUser;
    private Child child;
    private ChildNote note;
    private NoteComment topLevelComment1;
    private NoteComment topLevelComment2;
    private NoteComment reply1;
    private NoteComment reply2;

    @BeforeEach
    void setUp() {
        // 1. 사용자 생성
        primaryParent = createUser("primary@test.com", "주보호자", Set.of(UserRole.PARENT));
        therapist = createUser("therapist@test.com", "치료사", Set.of(UserRole.THERAPIST));
        unauthorizedUser = createUser("unauthorized@test.com", "권한없는 사용자", Set.of(UserRole.PARENT));

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
        em.persist(child);

        ChildrenAuthorizedUser therapistAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .isActive(true)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .build();
        child.addAuthorizedUser(therapistAuth);
        em.persist(therapistAuth);

        // 4. 노트 생성
        note = ChildNote.builder()
                .child(child)
                .author(primaryParent)
                .type(NoteType.PARENT_NOTE)
                .title("테스트 노트")
                .content("댓글 테스트")
                .isDeleted(false)
                .build();
        em.persist(note);

        // 5. 최상위 댓글 생성
        topLevelComment1 = NoteComment.builder()
                .note(note)
                .author(primaryParent)
                .content("부모의 첫 번째 댓글")
                .isDeleted(false)
                .build();
        em.persist(topLevelComment1);

        topLevelComment2 = NoteComment.builder()
                .note(note)
                .author(therapist)
                .content("치료사의 댓글")
                .isDeleted(false)
                .build();
        em.persist(topLevelComment2);

        // 6. 대댓글 생성
        reply1 = NoteComment.builder()
                .note(note)
                .author(therapist)
                .parentComment(topLevelComment1)
                .content("치료사의 대댓글 1")
                .isDeleted(false)
                .build();
        topLevelComment1.addReply(reply1);
        em.persist(reply1);

        reply2 = NoteComment.builder()
                .note(note)
                .author(primaryParent)
                .parentComment(topLevelComment1)
                .content("부모의 대댓글 2")
                .isDeleted(false)
                .build();
        topLevelComment1.addReply(reply2);
        em.persist(reply2);

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
    @DisplayName("권한이 있으면 댓글을 조회할 수 있다")
    void findByIdWithAuth_WithPermission_Success() {
        // when
        Optional<NoteComment> result = commentRepository.findByIdWithAuth(
                topLevelComment1.getCommentId(),
                primaryParent.getUserId()
        );

        // then
        assertThat(result.isPresent());
        assertThat(result.get().getContent()).isEqualTo("부모의 첫 번째 댓글");
    }

    @Test
    @DisplayName("권한이 없으면 댓글을 조회할 수 없다")
    void findByIdWithAuth_WithoutPermission_Empty() {
        // when
        Optional<NoteComment> result = commentRepository.findByIdWithAuth(
                topLevelComment1.getCommentId(),
                unauthorizedUser.getUserId()
        );

        // then
        assertThat(result).isEmpty();
    }

    // ============= 목록 조회 테스트 =============

    @Test
    @DisplayName("노트의 모든 댓글을 조회할 수 있다 (최상위 + 대댓글)")
    void findByNoteIdWithAuth_AllComments_Success() {
        // when
        List<NoteComment> result = commentRepository.findByNoteIdWithAuth(
                note.getNoteId(),
                primaryParent.getUserId()
        );

        // then
        assertThat(result).hasSize(4);
    }

    @Test
    @DisplayName("최상위 댓글만 페이징하여 조회할 수 있다")
    void findTopLevelByNoteIdWithAuth_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<NoteComment> result = commentRepository.findTopLevelByNoteIdWithAuth(
                note.getNoteId(),
                primaryParent.getUserId(),
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(comment -> comment.getParentComment() == null);
    }

    @Test
    @DisplayName("특정 댓글의 대댓글 목록을 조회할 수 있다")
    void findRepliesByParentIdWithAuth_Success() {
        // when
        List<NoteComment> result = commentRepository.findRepliesByParentIdWithAuth(
                topLevelComment1.getCommentId(),
                primaryParent.getUserId()
        );

        // then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting("content")
                .containsExactlyInAnyOrder("치료사의 대댓글 1", "부모의 대댓글 2");
    }

    @Test
    @DisplayName("권한이 없으면 댓글 목록이 비어있다")
    void findByNoteIdWithAuth_NoPermission_Empty() {
        // when
        List<NoteComment> result = commentRepository.findByNoteIdWithAuth(
                note.getNoteId(),
                unauthorizedUser.getUserId()
        );

        // then
        assertThat(result).isEmpty();
    }

    // ============= 통계 테스트 =============

    @Test
    @DisplayName("노트의 총 댓글 개수를 조회할 수 있다 (대댓글 포함)")
    void countByNoteId_Success() {
        // when
        long count = commentRepository.countByNoteId(note.getNoteId());

        // then
        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("최상위 댓글 개수를 조회할 수 있다")
    void countTopLevelByNoteId_Success() {
        // when
        long count = commentRepository.countTopLevelByNoteId(note.getNoteId());

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("특정 댓글의 대댓글 개수를 조회할 수 있다")
    void countRepliesByParentId_Success() {
        // when
        long count = commentRepository.countRepliesByParentId(topLevelComment1.getCommentId());

        // then
        assertThat(count).isEqualTo(2);
    }

    // ============= Soft Delete 테스트 =============

    @Test
    @DisplayName("삭제된 댓글은 조회되지 않는다")
    void findByIdWithAuth_DeletedComment_Empty() {
        // given
        topLevelComment1.delete();
        em.merge(topLevelComment1);

        em.flush();
        em.clear();

        // when
        Optional<NoteComment> result = commentRepository.findByIdWithAuth(
                topLevelComment1.getCommentId(),
                primaryParent.getUserId()
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("삭제된 댓글은 목록에서 제외된다")
    void findByNoteIdWithAuth_DeletedComment_Excluded() {
        // given
        topLevelComment2.delete();
        em.merge(topLevelComment2);

        em.flush();
        em.clear();

        // when
        List<NoteComment> result = commentRepository.findByNoteIdWithAuth(
                note.getNoteId(),
                primaryParent.getUserId()
        );

        // then
        assertThat(result).hasSize(3); // 최상위 1개 + 대댓글 2개
    }

    // ============= 계층 구조 테스트 =============

    @Test
    @DisplayName("대댓글이 없는 댓글은 replies가 비어있다")
    void findByNoteIdWithAuth_NoReplies_EmptyList() {
        // when
        List<NoteComment> result = commentRepository.findByNoteIdWithAuth(
                note.getNoteId(),
                primaryParent.getUserId()
        );

        NoteComment comment2 = result.stream()
                .filter(c -> c.getCommentId().equals(topLevelComment2.getCommentId()))
                .findFirst()
                .orElseThrow();

        // then
        assertThat(comment2.getReplies()).isEmpty();
    }

    @Test
    @DisplayName("최상위 댓글을 삭제하면 대댓글도 함께 삭제된다 (CASCADE)")
    void deleteTopLevelComment_CascadeToReplies() {
        // given
        topLevelComment1.delete();
        em.merge(topLevelComment1);

        em.flush();
        em.clear();

        // when
        long totalCount = commentRepository.countByNoteId(note.getNoteId());

        // then
        assertThat(totalCount).isEqualTo(1); // topLevelComment2만 남음
    }

    // ============= 관리자용 메서드 테스트 =============

    @Test
    @DisplayName("작성자별로 모든 댓글을 조회할 수 있다")
    void findAllByAuthorId_Success() {
        // when
        List<NoteComment> result = commentRepository.findAllByAuthorId(therapist.getUserId());

        // then
        assertThat(result).hasSize(2); // 최상위 1개 + 대댓글 1개
        assertThat(result)
                .allMatch(comment -> comment.getAuthor().getUserId().equals(therapist.getUserId()));
    }
}
