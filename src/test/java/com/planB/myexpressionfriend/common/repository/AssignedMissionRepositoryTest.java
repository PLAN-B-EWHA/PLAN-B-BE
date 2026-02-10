package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.child.ChildrenAuthorizedUser;
import com.planB.myexpressionfriend.common.domain.mission.*;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("AssignedMissionRepository 테스트")
@Transactional
public class AssignedMissionRepositoryTest {

    @Autowired
    private AssignedMissionRepository missionRepository;

    @Autowired
    private TestEntityManager em;

    private User primaryParent;
    private User therapist;
    private User unauthorizedUser;
    private Child child;
    private MissionTemplate template1;
    private MissionTemplate template2;
    private AssignedMission assignedMission;
    private AssignedMission inProgressMission;
    private AssignedMission completedMission;
    private AssignedMission overdueMission;

    @BeforeEach
    void setUp() {
        // 1. 사용자 생성
        primaryParent = createUser("parent@test.com", "주보호자", Set.of(UserRole.PARENT));
        therapist = createUser("therapist@test.com", "치료사", Set.of(UserRole.THERAPIST));
        unauthorizedUser = createUser("unauthorized@test.com", "권한없는사용자", Set.of(UserRole.PARENT));

        // 2. 아동 생성
        child = Child.builder()
                .name("테스트아동")
                .birthDate(LocalDate.of(2020, 1, 1))
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

        ChildrenAuthorizedUser therapistAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .isActive(true)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .build();
        child.addAuthorizedUser(therapistAuth);
        em.persist(therapistAuth);

        // 4. 미션 템플릿 생성
        template1 = MissionTemplate.builder()
                .title("기쁨 표정 짓기")
                .description("표정 연습")
                .category(MissionCategory.EXPRESSION)
                .difficulty(MissionDifficulty.BEGINNER)
                .instructions("연습하세요")
                .expectedDuration(10)
                .llmGenerated(false)
                .active(true)
                .isDeleted(false)
                .build();
        em.persist(template1);

        template2 = MissionTemplate.builder()
                .title("감정 인식하기")
                .description("감정 훈련")
                .category(MissionCategory.EMOTION_RECOGNITION)
                .difficulty(MissionDifficulty.INTERMEDIATE)
                .instructions("카드를 보세요")
                .expectedDuration(15)
                .llmGenerated(false)
                .active(true)
                .isDeleted(false)
                .build();
        em.persist(template2);

        // 5. 할당된 미션 생성
        assignedMission = AssignedMission.builder()
                .child(child)
                .therapist(therapist)
                .template(template1)
                .status(MissionStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(7))
                .isDeleted(false)
                .build();
        em.persist(assignedMission);

        inProgressMission = AssignedMission.builder()
                .child(child)
                .therapist(therapist)
                .template(template2)
                .status(MissionStatus.IN_PROGRESS)
                .assignedAt(LocalDateTime.now().minusDays(2))
                .startedAt(LocalDateTime.now().minusDays(1))
                .dueDate(LocalDateTime.now().plusDays(5))
                .isDeleted(false)
                .build();
        em.persist(inProgressMission);

        completedMission = AssignedMission.builder()
                .child(child)
                .therapist(therapist)
                .template(template1)
                .status(MissionStatus.COMPLETED)
                .assignedAt(LocalDateTime.now().minusDays(5))
                .startedAt(LocalDateTime.now().minusDays(3))
                .completedAt(LocalDateTime.now().minusDays(1))
                .parentNote("잘 완료했습니다")
                .isDeleted(false)
                .build();
        em.persist(completedMission);

        overdueMission = AssignedMission.builder()
                .child(child)
                .therapist(therapist)
                .template(template1)
                .status(MissionStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now().minusDays(10))
                .dueDate(LocalDateTime.now().minusDays(2)) // 마감일 지남
                .isDeleted(false)
                .build();
        em.persist(overdueMission);

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
    @DisplayName("권한이 있으면 미션을 조회할 수 있다")
    void findByIdWithAuth_WithPermission_Success() {
        // when
        Optional<AssignedMission> result = missionRepository.findByIdWithAuth(
                assignedMission.getMissionId(),
                primaryParent.getUserId()
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTemplate().getTitle()).isEqualTo("기쁨 표정 짓기");
    }

    @Test
    @DisplayName("권한이 없으면 미션을 조회할 수 없다")
    void findByIdWithAuth_NoPermission_Empty() {
        // when
        Optional<AssignedMission> result = missionRepository.findByIdWithAuth(
                assignedMission.getMissionId(),
                unauthorizedUser.getUserId()
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("치료사도 자신이 할당한 미션을 조회할 수 있다")
    void findByIdWithAuth_Therapist_Success() {
        // when
        Optional<AssignedMission> result = missionRepository.findByIdWithAuth(
                assignedMission.getMissionId(),
                therapist.getUserId()
        );

        // then
        assertThat(result).isPresent();
    }

    // ============= 목록 조회 테스트 =============

    @Test
    @DisplayName("아동의 모든 미션을 조회할 수 있다")
    void findByChildIdWithAuth_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<AssignedMission> result = missionRepository.findByChildIdWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(4);
    }

    @Test
    @DisplayName("권한이 없으면 미션 목록이 비어있다")
    void findByChildIdWithAuth_NoPermission_Empty() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<AssignedMission> result = missionRepository.findByChildIdWithAuth(
                child.getChildId(),
                unauthorizedUser.getUserId(),
                pageRequest
        );

        // then
        assertThat(result.getContent()).isEmpty();
    }

    // ============= 상태별 조회 테스트 =============

    @Test
    @DisplayName("특정 상태의 미션만 조회할 수 있다")
    void findByChildIdAndStatusWithAuth_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<AssignedMission> assignedOnly = missionRepository.findByChildIdAndStatusWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                MissionStatus.ASSIGNED,
                pageRequest
        );

        Page<AssignedMission> completedOnly = missionRepository.findByChildIdAndStatusWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                MissionStatus.COMPLETED,
                pageRequest
        );

        // then
        assertThat(assignedOnly.getContent()).hasSize(2); // assignedMission, overdueMission
        assertThat(completedOnly.getContent()).hasSize(1);
    }

    // ============= 치료사별 조회 테스트 =============

    @Test
    @DisplayName("특정 치료사가 할당한 미션을 조회할 수 있다")
    void findByChildIdAndTherapistWithAuth_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<AssignedMission> result = missionRepository.findByChildIdAndTherapistWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                therapist.getUserId(),
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(4);
        assertThat(result.getContent())
                .allMatch(mission -> mission.getTherapist().getUserId().equals(therapist.getUserId()));
    }

    // ============= 날짜 범위 조회 테스트 =============

    @Test
    @DisplayName("날짜 범위로 미션을 조회할 수 있다")
    void findByChildIdAndDateRangeWithAuth_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);
        LocalDateTime startDate = LocalDateTime.now().minusDays(3);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        // when
        Page<AssignedMission> result = missionRepository.findByChildIdAndDateRangeWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                startDate,
                endDate,
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(2); // assignedMission, inProgressMission
    }

    // ============= 마감일 관련 테스트 =============

    @Test
    @DisplayName("마감일이 지난 미션을 조회할 수 있다")
    void findOverdueMissionsWithAuth_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<AssignedMission> result = missionRepository.findOverdueMissionsWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                LocalDateTime.now(),
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMissionId())
                .isEqualTo(overdueMission.getMissionId());
    }

    @Test
    @DisplayName("완료된 미션은 마감일이 지나도 조회되지 않는다")
    void findOverdueMissionsWithAuth_ExcludeCompleted() {
        // given
        // ReflectionTestUtils를 사용하여 검증 로직을 우회하여 과거 날짜 주입
        org.springframework.test.util.ReflectionTestUtils.setField(
                completedMission, "dueDate", LocalDateTime.now().minusDays(1));

        completedMission.setStatus(MissionStatus.COMPLETED); // 상태 확인
        em.merge(completedMission);
        em.flush();
        em.clear();

        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<AssignedMission> result = missionRepository.findOverdueMissionsWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                LocalDateTime.now(),
                pageRequest
        );

        // then
        assertThat(result.getContent())
                .noneMatch(mission -> mission.getMissionId().equals(completedMission.getMissionId()));
    }

    // ============= 완료 대기 미션 테스트 =============

    @Test
    @DisplayName("완료 대기중인 미션을 조회할 수 있다")
    void findPendingVerificationWithAuth_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<AssignedMission> result = missionRepository.findPendingVerificationWithAuth(
                child.getChildId(),
                therapist.getUserId(),
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(MissionStatus.COMPLETED);
    }

    // ============= 통계 테스트 =============

    @Test
    @DisplayName("아동의 미션 총 개수를 조회할 수 있다")
    void countByChildIdWithAuth_Success() {
        // when
        long count = missionRepository.countByChildIdWithAuth(
                child.getChildId(),
                primaryParent.getUserId()
        );

        // then
        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("상태별 미션 개수를 조회할 수 있다")
    void countByChildIdAndStatusWithAuth_Success() {
        // when
        long assignedCount = missionRepository.countByChildIdAndStatusWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                MissionStatus.ASSIGNED
        );

        long completedCount = missionRepository.countByChildIdAndStatusWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                MissionStatus.COMPLETED
        );

        // then
        assertThat(assignedCount).isEqualTo(2);
        assertThat(completedCount).isEqualTo(1);
    }

    // ============= Soft Delete 테스트 =============

    @Test
    @DisplayName("삭제된 미션은 조회되지 않는다")
    void findByIdWithAuth_DeletedMission_Empty() {
        // given
        assignedMission.delete();
        em.merge(assignedMission);
        em.flush();
        em.clear();

        // when
        Optional<AssignedMission> result = missionRepository.findByIdWithAuth(
                assignedMission.getMissionId(),
                primaryParent.getUserId()
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("삭제된 미션은 목록에서 제외된다")
    void findByChildIdWithAuth_DeletedMission_Excluded() {
        // given
        assignedMission.delete();
        em.merge(assignedMission);
        em.flush();
        em.clear();

        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<AssignedMission> result = missionRepository.findByChildIdWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(3);
    }

    // ============= 관리자용 메서드 테스트 =============

    @Test
    @DisplayName("아동의 모든 미션을 조회할 수 있다 (권한 검증 없음)")
    void findAllByChildId_Success() {
        // when
        List<AssignedMission> result = missionRepository.findAllByChildId(child.getChildId());

        // then
        assertThat(result).hasSize(4);
    }

    @Test
    @DisplayName("치료사가 할당한 모든 미션을 조회할 수 있다 (권한 검증 없음)")
    void findAllByTherapistId_Success() {
        // when
        List<AssignedMission> result = missionRepository.findAllByTherapistId(therapist.getUserId());

        // then
        assertThat(result).hasSize(4);
        assertThat(result)
                .allMatch(mission -> mission.getTherapist().getUserId().equals(therapist.getUserId()));
    }
}