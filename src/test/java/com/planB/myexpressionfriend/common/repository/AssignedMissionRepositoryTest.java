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
@DisplayName("AssignedMissionRepository ?뚯뒪??)
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
        // 1. ?ъ슜???앹꽦
        primaryParent = createUser("parent@test.com", "二쇰낫?몄옄", Set.of(UserRole.PARENT));
        therapist = createUser("therapist@test.com", "移섎즺??, Set.of(UserRole.THERAPIST));
        unauthorizedUser = createUser("unauthorized@test.com", "沅뚰븳?녿뒗?ъ슜??, Set.of(UserRole.PARENT));

        // 2. ?꾨룞 ?앹꽦
        child = Child.builder()
                .name("?뚯뒪?몄븘??)
                .birthDate(LocalDate.of(2020, 1, 1))
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

        ChildrenAuthorizedUser therapistAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .isActive(true)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .build();
        child.addAuthorizedUser(therapistAuth);
        em.persist(therapistAuth);

        // 4. 誘몄뀡 ?쒗뵆由??앹꽦
        template1 = MissionTemplate.builder()
                .title("湲곗겏 ?쒖젙 吏볤린")
                .description("?쒖젙 ?곗뒿")
                .category(MissionCategory.EXPRESSION)
                .difficulty(MissionDifficulty.BEGINNER)
                .instructions("?곗뒿?섏꽭??)
                .expectedDuration(10)
                .llmGenerated(false)
                .active(true)
                .isDeleted(false)
                .build();
        em.persist(template1);

        template2 = MissionTemplate.builder()
                .title("감정 인식?섍린")
                .description("媛먯젙 ?덈젴")
                .category(MissionCategory.EMOTION_RECOGNITION)
                .difficulty(MissionDifficulty.INTERMEDIATE)
                .instructions("移대뱶瑜?蹂댁꽭??)
                .expectedDuration(15)
                .llmGenerated(false)
                .active(true)
                .isDeleted(false)
                .build();
        em.persist(template2);

        // 5. 할당됨誘몄뀡 ?앹꽦
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
                .parentNote("??완료?덉뒿?덈떎")
                .isDeleted(false)
                .build();
        em.persist(completedMission);

        overdueMission = AssignedMission.builder()
                .child(child)
                .therapist(therapist)
                .template(template1)
                .status(MissionStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now().minusDays(10))
                .dueDate(LocalDateTime.now().minusDays(2)) // 留덇컧??吏??
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

    // ============= 沅뚰븳 寃利??뚯뒪??=============

    @Test
    @DisplayName("沅뚰븳???덉쑝硫?誘몄뀡??議고쉶?????덈떎")
    void findByIdWithAuth_WithPermission_Success() {
        // when
        Optional<AssignedMission> result = missionRepository.findByIdWithAuth(
                assignedMission.getMissionId(),
                primaryParent.getUserId()
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTemplate().getTitle()).isEqualTo("湲곗겏 ?쒖젙 吏볤린");
    }

    @Test
    @DisplayName("沅뚰븳???놁쑝硫?誘몄뀡??議고쉶?????녿떎")
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
    @DisplayName("移섎즺?щ룄 ?먯떊??할당됨誘몄뀡??議고쉶?????덈떎")
    void findByIdWithAuth_Therapist_Success() {
        // when
        Optional<AssignedMission> result = missionRepository.findByIdWithAuth(
                assignedMission.getMissionId(),
                therapist.getUserId()
        );

        // then
        assertThat(result).isPresent();
    }

    // ============= 紐⑸줉 議고쉶 ?뚯뒪??=============

    @Test
    @DisplayName("?꾨룞??紐⑤뱺 誘몄뀡??議고쉶?????덈떎")
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
    @DisplayName("沅뚰븳???놁쑝硫?誘몄뀡 紐⑸줉??鍮꾩뼱?덈떎")
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

    // ============= ?곹깭蹂?議고쉶 ?뚯뒪??=============

    @Test
    @DisplayName("?뱀젙 ?곹깭??誘몄뀡留?議고쉶?????덈떎")
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

    // ============= 移섎즺?щ퀎 議고쉶 ?뚯뒪??=============

    @Test
    @DisplayName("?뱀젙 移섎즺?ш? 할당됨誘몄뀡??議고쉶?????덈떎")
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

    // ============= ?좎쭨 踰붿쐞 議고쉶 ?뚯뒪??=============

    @Test
    @DisplayName("?좎쭨 踰붿쐞濡?誘몄뀡??議고쉶?????덈떎")
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

    // ============= 留덇컧??愿???뚯뒪??=============

    @Test
    @DisplayName("留덇컧?쇱씠 吏??誘몄뀡??議고쉶?????덈떎")
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
    @DisplayName("완료??誘몄뀡? 留덇컧?쇱씠 吏?섎룄 議고쉶?섏? ?딅뒗??)
    void findOverdueMissionsWithAuth_ExcludeCompleted() {
        // given
        // ReflectionTestUtils瑜??ъ슜?섏뿬 寃利?濡쒖쭅???고쉶?섏뿬 怨쇨굅 ?좎쭨 二쇱엯
        org.springframework.test.util.ReflectionTestUtils.setField(
                completedMission, "dueDate", LocalDateTime.now().minusDays(1));

        completedMission.complete("finished");
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

    // ============= 완료 ?湲?誘몄뀡 ?뚯뒪??=============

    @Test
    @DisplayName("완료 ?湲곗쨷??誘몄뀡??議고쉶?????덈떎")
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

    // ============= ?듦퀎 ?뚯뒪??=============

    @Test
    @DisplayName("?꾨룞??誘몄뀡 珥?媛쒖닔瑜?議고쉶?????덈떎")
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
    @DisplayName("?곹깭蹂?誘몄뀡 媛쒖닔瑜?議고쉶?????덈떎")
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

    // ============= Soft Delete ?뚯뒪??=============

    @Test
    @DisplayName("??젣??誘몄뀡? 議고쉶?섏? ?딅뒗??)
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
    @DisplayName("??젣??誘몄뀡? 紐⑸줉?먯꽌 ?쒖쇅?쒕떎")
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

    // ============= 愿由ъ옄??硫붿꽌???뚯뒪??=============

    @Test
    @DisplayName("?꾨룞??紐⑤뱺 誘몄뀡??議고쉶?????덈떎 (沅뚰븳 寃利??놁쓬)")
    void findAllByChildId_Success() {
        // when
        List<AssignedMission> result = missionRepository.findAllByChildId(child.getChildId());

        // then
        assertThat(result).hasSize(4);
    }

    @Test
    @DisplayName("移섎즺?ш? 할당됨紐⑤뱺 誘몄뀡??議고쉶?????덈떎 (沅뚰븳 寃利??놁쓬)")
    void findAllByTherapistId_Success() {
        // when
        List<AssignedMission> result = missionRepository.findAllByTherapistId(therapist.getUserId());

        // then
        assertThat(result).hasSize(4);
        assertThat(result)
                .allMatch(mission -> mission.getTherapist().getUserId().equals(therapist.getUserId()));
    }
}