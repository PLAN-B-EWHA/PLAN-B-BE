package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.child.ChildrenAuthorizedUser;
import com.planB.myexpressionfriend.common.domain.mission.AssignedMission;
import com.planB.myexpressionfriend.common.domain.mission.MissionCategory;
import com.planB.myexpressionfriend.common.domain.mission.MissionDifficulty;
import com.planB.myexpressionfriend.common.domain.mission.MissionStatus;
import com.planB.myexpressionfriend.common.domain.mission.MissionTemplate;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("AssignedMission Repository 테스트")
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
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .build();
        child.addAuthorizedUser(therapistAuth);
        em.persist(therapistAuth);

        // 4. 미션 템플릿 생성
        template1 = MissionTemplate.builder()
                .title("표정 따라하기")
                .description("기본 표정 훈련 미션")
                .category(MissionCategory.EXPRESSION)
                .difficulty(MissionDifficulty.BEGINNER)
                .instructions("안내에 따라 표정을 따라해 보세요")
                .expectedDuration(10)
                .llmGenerated(false)
                .active(true)
                .isDeleted(false)
                .build();
        em.persist(template1);

        template2 = MissionTemplate.builder()
                .title("감정 인식 퀴즈")
                .description("상황에 맞는 감정을 선택하는 미션")
                .category(MissionCategory.EMOTION_RECOGNITION)
                .difficulty(MissionDifficulty.INTERMEDIATE)
                .instructions("화면에 나온 감정을 골라보세요")
                .expectedDuration(15)
                .llmGenerated(false)
                .active(true)
                .isDeleted(false)
                .build();
        em.persist(template2);

        // 5. 미션 배정 데이터 생성
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
                .parentNote("가정에서 수행 결과를 기록했습니다.")
                .isDeleted(false)
                .build();
        em.persist(completedMission);

        overdueMission = AssignedMission.builder()
                .child(child)
                .therapist(therapist)
                .template(template1)
                .status(MissionStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now().minusDays(10))
                .dueDate(LocalDateTime.now().minusDays(2))
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

    // ============= 상세 조회 =============

    @Test
    @DisplayName("권한이 있으면 미션 상세 조회 성공")
    void findByIdWithAuth_WithPermission_Success() {
        Optional<AssignedMission> result = missionRepository.findByIdWithAuth(
                assignedMission.getMissionId(),
                primaryParent.getUserId()
        );

        assertThat(result).isPresent();
        assertThat(result.get().getTemplate().getTitle()).isEqualTo("표정 따라하기");
    }

    @Test
    @DisplayName("권한이 없으면 미션 상세 조회 실패")
    void findByIdWithAuth_NoPermission_Empty() {
        Optional<AssignedMission> result = missionRepository.findByIdWithAuth(
                assignedMission.getMissionId(),
                unauthorizedUser.getUserId()
        );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("치료사도 배정된 미션을 조회할 수 있다")
    void findByIdWithAuth_Therapist_Success() {
        Optional<AssignedMission> result = missionRepository.findByIdWithAuth(
                assignedMission.getMissionId(),
                therapist.getUserId()
        );

        assertThat(result).isPresent();
    }

    // ============= 목록 조회 =============

    @Test
    @DisplayName("아동별 미션 목록 조회 성공")
    void findByChildIdWithAuth_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<AssignedMission> result = missionRepository.findByChildIdWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                pageRequest
        );

        assertThat(result.getContent()).hasSize(4);
    }

    @Test
    @DisplayName("권한이 없으면 아동별 미션 목록은 비어 있다")
    void findByChildIdWithAuth_NoPermission_Empty() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<AssignedMission> result = missionRepository.findByChildIdWithAuth(
                child.getChildId(),
                unauthorizedUser.getUserId(),
                pageRequest
        );

        assertThat(result.getContent()).isEmpty();
    }

    // ============= 필터 조회 =============

    @Test
    @DisplayName("상태별 미션 조회 성공")
    void findByChildIdAndStatusWithAuth_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

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

        assertThat(assignedOnly.getContent()).hasSize(2);
        assertThat(completedOnly.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("치료사 기준 미션 조회 성공")
    void findByChildIdAndTherapistWithAuth_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<AssignedMission> result = missionRepository.findByChildIdAndTherapistWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                therapist.getUserId(),
                pageRequest
        );

        assertThat(result.getContent()).hasSize(4);
        assertThat(result.getContent())
                .allMatch(mission -> mission.getTherapist().getUserId().equals(therapist.getUserId()));
    }

    @Test
    @DisplayName("날짜 범위로 미션 조회 성공")
    void findByChildIdAndDateRangeWithAuth_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        LocalDateTime startDate = LocalDateTime.now().minusDays(3);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        Page<AssignedMission> result = missionRepository.findByChildIdAndDateRangeWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                startDate,
                endDate,
                pageRequest
        );

        assertThat(result.getContent()).hasSize(2);
    }

    // ============= 기한 초과/검토 대기 =============

    @Test
    @DisplayName("기한 초과 미션 조회 성공")
    void findOverdueMissionsWithAuth_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<AssignedMission> result = missionRepository.findOverdueMissionsWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                LocalDateTime.now(),
                pageRequest
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMissionId()).isEqualTo(overdueMission.getMissionId());
    }

    @Test
    @DisplayName("완료된 미션은 기한 초과 목록에서 제외된다")
    void findOverdueMissionsWithAuth_ExcludeCompleted() {
        org.springframework.test.util.ReflectionTestUtils.setField(
                completedMission, "dueDate", LocalDateTime.now().minusDays(1));

        completedMission.complete("finished");
        em.merge(completedMission);
        em.flush();
        em.clear();

        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<AssignedMission> result = missionRepository.findOverdueMissionsWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                LocalDateTime.now(),
                pageRequest
        );

        assertThat(result.getContent())
                .noneMatch(mission -> mission.getMissionId().equals(completedMission.getMissionId()));
    }

    @Test
    @DisplayName("검토 대기 미션 조회 성공")
    void findPendingVerificationWithAuth_Success() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<AssignedMission> result = missionRepository.findPendingVerificationWithAuth(
                child.getChildId(),
                therapist.getUserId(),
                pageRequest
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(MissionStatus.COMPLETED);
    }

    // ============= 통계 =============

    @Test
    @DisplayName("아동별 미션 수 조회 성공")
    void countByChildIdWithAuth_Success() {
        long count = missionRepository.countByChildIdWithAuth(
                child.getChildId(),
                primaryParent.getUserId()
        );

        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("상태별 미션 수 조회 성공")
    void countByChildIdAndStatusWithAuth_Success() {
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

        assertThat(assignedCount).isEqualTo(2);
        assertThat(completedCount).isEqualTo(1);
    }

    // ============= Soft Delete =============

    @Test
    @DisplayName("삭제된 미션은 상세 조회에서 제외된다")
    void findByIdWithAuth_DeletedMission_Empty() {
        assignedMission.delete(primaryParent.getUserId());
        em.merge(assignedMission);
        em.flush();
        em.clear();

        Optional<AssignedMission> result = missionRepository.findByIdWithAuth(
                assignedMission.getMissionId(),
                primaryParent.getUserId()
        );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("삭제된 미션은 목록 조회에서 제외된다")
    void findByChildIdWithAuth_DeletedMission_Excluded() {
        assignedMission.delete(primaryParent.getUserId());
        em.merge(assignedMission);
        em.flush();
        em.clear();

        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<AssignedMission> result = missionRepository.findByChildIdWithAuth(
                child.getChildId(),
                primaryParent.getUserId(),
                pageRequest
        );

        assertThat(result.getContent()).hasSize(3);
    }

    // ============= 관리자 조회 =============

    @Test
    @DisplayName("아동별 전체 미션 조회 성공")
    void findAllByChildId_Success() {
        List<AssignedMission> result = missionRepository.findAllByChildId(child.getChildId());

        assertThat(result).hasSize(4);
    }

    @Test
    @DisplayName("치료사별 전체 미션 조회 성공")
    void findAllByTherapistId_Success() {
        List<AssignedMission> result = missionRepository.findAllByTherapistId(therapist.getUserId());

        assertThat(result).hasSize(4);
        assertThat(result)
                .allMatch(mission -> mission.getTherapist().getUserId().equals(therapist.getUserId()));
    }
}
