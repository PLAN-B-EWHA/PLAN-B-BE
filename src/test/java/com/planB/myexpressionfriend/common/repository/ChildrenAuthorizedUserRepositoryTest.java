package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.child.ChildrenAuthorizedUser;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Slf4j
@DisplayName("ChildrenAuthorizedUser Repository 테스트")
@Transactional
public class ChildrenAuthorizedUserRepositoryTest {

    @Autowired
    private ChildrenAuthorizedUserRepository authorizedUserRepository;

    @Autowired
    private ChildRepository childRepository;

    @Autowired
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private User parent;
    private User therapist;
    private Child child;

    @BeforeEach
    void setUp() {
        parent = User.builder()
                .email("parent@test.com")
                .password(passwordEncoder.encode("password"))
                .name("부모")
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(parent);

        therapist = User.builder()
                .email("therapist@test.com")
                .password(passwordEncoder.encode("password"))
                .name("치료사")
                .roles(Set.of(UserRole.THERAPIST))
                .build();
        userRepository.save(therapist);

        child = Child.builder()
                .name("테스트아동")
                .birthDate(LocalDate.of(2020, 5, 15))
                .gender("MALE")
                .build();
        childRepository.save(child);
    }

    @Test
    @DisplayName("주보호자 권한 저장")
    void testInsertPrimaryAuthorization() {
        ChildrenAuthorizedUser authorization = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .authorizedBy(parent)
                .isActive(true)
                .build();

        child.addAuthorizedUser(authorization);
        ChildrenAuthorizedUser saved = authorizedUserRepository.save(authorization);

        assertThat(saved.getAuthorizationId()).isNotNull();
        assertThat(saved.getIsPrimary()).isTrue();
        assertThat(saved.getIsActive()).isTrue();
        assertThat(saved.getPermissions()).containsExactlyInAnyOrder(ChildPermissionType.values());
        assertThat(saved.hasPermission(ChildPermissionType.PLAY_GAME)).isTrue();
        assertThat(saved.hasPermission(ChildPermissionType.VIEW_REPORT)).isTrue();

        log.info("주보호자 권한 저장 완료: {}", saved);
    }

    @Test
    @DisplayName("치료사 권한 저장")
    void testInsertTherapistAuthorization() {
        ChildrenAuthorizedUser authorization = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .permissions(Set.of(
                        ChildPermissionType.VIEW_REPORT,
                        ChildPermissionType.WRITE_NOTE
                ))
                .authorizedBy(parent)
                .isActive(true)
                .build();

        child.addAuthorizedUser(authorization);
        ChildrenAuthorizedUser saved = authorizedUserRepository.save(authorization);

        assertThat(saved.getIsPrimary()).isFalse();
        assertThat(saved.getPermissions()).hasSize(2);
        assertThat(saved.hasPermission(ChildPermissionType.VIEW_REPORT)).isTrue();
        assertThat(saved.hasPermission(ChildPermissionType.WRITE_NOTE)).isTrue();
        assertThat(saved.hasPermission(ChildPermissionType.PLAY_GAME)).isFalse();

        log.info("치료사 권한 저장 완료: {}", saved);
    }

    @Test
    @DisplayName("아동과 사용자로 권한 조회")
    void testFindByChildAndUser() {
        ChildrenAuthorizedUser authorization = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .isActive(true)
                .build();

        child.addAuthorizedUser(authorization);
        authorizedUserRepository.save(authorization);

        Optional<ChildrenAuthorizedUser> found = authorizedUserRepository.findByChildAndUser(child, parent);

        assertThat(found).isPresent();
        assertThat(found.get().getIsPrimary()).isTrue();
        assertThat(found.get().getUser().getEmail()).isEqualTo("parent@test.com");

        log.info("권한 조회 성공: {}", found.get());
    }

    @Test
    @DisplayName("아동 ID와 사용자 ID로 권한 조회")
    void testFindByChildIdAndUserId() {
        ChildrenAuthorizedUser authorization = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(true)
                .build();
        child.addAuthorizedUser(authorization);
        authorizedUserRepository.save(authorization);

        Optional<ChildrenAuthorizedUser> found = authorizedUserRepository
                .findByChildIdAndUserId(child.getChildId(), therapist.getUserId());

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getUserId()).isEqualTo(therapist.getUserId());
        assertThat(found.get().hasPermission(ChildPermissionType.VIEW_REPORT)).isTrue();

        log.info("ID 기반 권한 조회 성공: {}", found.get());
    }

    @Test
    @DisplayName("활성 권한만 조회")
    public void testFindActiveByChildId() {
        ChildrenAuthorizedUser parentAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .isActive(true)
                .build();
        child.addAuthorizedUser(parentAuth);

        ChildrenAuthorizedUser therapistAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(true)
                .build();
        child.addAuthorizedUser(therapistAuth);

        User inactiveUser = User.builder()
                .email("inactive@test.com")
                .password(passwordEncoder.encode("password"))
                .name("비활성사용자")
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(inactiveUser);

        ChildrenAuthorizedUser inactiveAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(inactiveUser)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(false)
                .build();
        child.addAuthorizedUser(inactiveAuth);

        authorizedUserRepository.saveAll(List.of(parentAuth, therapistAuth, inactiveAuth));

        List<ChildrenAuthorizedUser> activeAuths = authorizedUserRepository.findActiveByChildId(child.getChildId());

        assertThat(activeAuths).hasSize(2);
        assertThat(activeAuths).extracting(au -> au.getUser().getEmail())
                .containsExactlyInAnyOrder("parent@test.com", "therapist@test.com");
        assertThat(activeAuths.get(0).getIsPrimary()).isTrue();

        log.info("활성 권한 조회 결과: {}", activeAuths);
    }

    @Test
    @DisplayName("아동 ID로 주보호자 조회")
    public void testFindPrimaryByChildId() {
        ChildrenAuthorizedUser primaryAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .isActive(true)
                .build();
        child.addAuthorizedUser(primaryAuth);
        authorizedUserRepository.save(primaryAuth);

        Optional<ChildrenAuthorizedUser> primary = authorizedUserRepository
                .findPrimaryByChildId(child.getChildId());

        assertThat(primary).isPresent();
        assertThat(primary.get().getIsPrimary()).isTrue();
        assertThat(primary.get().getUser().getEmail()).isEqualTo("parent@test.com");

        log.info("주보호자 조회 결과: {}", primary.get());
    }

    @Test
    @DisplayName("사용자 ID로 주보호자 권한 조회")
    void testFindPrimaryByUserId() {
        Child child1 = Child.builder().name("첫째").build();
        Child child2 = Child.builder().name("둘째").build();
        childRepository.saveAll(List.of(child1, child2));

        ChildrenAuthorizedUser auth1 = ChildrenAuthorizedUser.builder()
                .child(child1)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .isActive(true)
                .build();
        child1.addAuthorizedUser(auth1);

        ChildrenAuthorizedUser auth2 = ChildrenAuthorizedUser.builder()
                .child(child2)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .isActive(true)
                .build();
        child2.addAuthorizedUser(auth2);

        authorizedUserRepository.saveAll(List.of(auth1, auth2));

        List<ChildrenAuthorizedUser> primaryAuths = authorizedUserRepository
                .findPrimaryByUserId(parent.getUserId());

        assertThat(primaryAuths).hasSize(2);
        assertThat(primaryAuths).allMatch(ChildrenAuthorizedUser::getIsPrimary);
        assertThat(primaryAuths).extracting(au -> au.getChild().getName())
                .containsExactlyInAnyOrder("첫째", "둘째");

        log.info("사용자 기준 주보호자 권한 조회 결과: {}", primaryAuths);
    }

    @Test
    @DisplayName("아동과 권한 유형으로 조회")
    void testFindByChildIdAndPermission() {
        User user1 = User.builder()
                .email("user1@test.com")
                .password(passwordEncoder.encode("password"))
                .name("사용자1")
                .roles(Set.of(UserRole.PARENT))
                .build();

        User user2 = User.builder()
                .email("user2@test.com")
                .password(passwordEncoder.encode("password"))
                .name("사용자2")
                .roles(Set.of(UserRole.THERAPIST))
                .build();

        userRepository.saveAll(List.of(user1, user2));

        ChildrenAuthorizedUser auth1 = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(user1)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.PLAY_GAME))
                .isActive(true)
                .build();
        child.addAuthorizedUser(auth1);

        ChildrenAuthorizedUser auth2 = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(user2)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(true)
                .build();
        child.addAuthorizedUser(auth2);

        authorizedUserRepository.saveAll(List.of(auth1, auth2));

        List<ChildrenAuthorizedUser> playGameUsers = authorizedUserRepository
                .findByChildIdAndPermission(child.getChildId(), ChildPermissionType.PLAY_GAME);

        List<ChildrenAuthorizedUser> viewReportUsers = authorizedUserRepository
                .findByChildIdAndPermission(child.getChildId(), ChildPermissionType.VIEW_REPORT);

        assertThat(playGameUsers).hasSize(1);
        assertThat(playGameUsers.get(0).getUser().getEmail()).isEqualTo("user1@test.com");
        assertThat(viewReportUsers).hasSize(1);
        assertThat(viewReportUsers.get(0).getUser().getEmail()).isEqualTo("user2@test.com");

        log.info("PLAY_GAME 권한 사용자: {}", playGameUsers);
        log.info("VIEW_REPORT 권한 사용자: {}", viewReportUsers);
    }

    @Test
    @DisplayName("권한 보유 여부 확인")
    void testExistsByChildIdAndUserIdAndPermission() {
        ChildrenAuthorizedUser authorization = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .permissions(Set.of(
                        ChildPermissionType.VIEW_REPORT,
                        ChildPermissionType.WRITE_NOTE
                ))
                .isActive(true)
                .build();
        child.addAuthorizedUser(authorization);
        authorizedUserRepository.save(authorization);

        boolean hasViewReport = authorizedUserRepository.existsByChildIdAndUserIdAndPermission(
                child.getChildId(),
                therapist.getUserId(),
                ChildPermissionType.VIEW_REPORT
        );
        assertThat(hasViewReport).isTrue();

        boolean hasPlayGame = authorizedUserRepository.existsByChildIdAndUserIdAndPermission(
                child.getChildId(),
                therapist.getUserId(),
                ChildPermissionType.PLAY_GAME
        );
        assertThat(hasPlayGame).isFalse();

        log.info("VIEW_REPORT 권한 여부: {}", hasViewReport);
        log.info("PLAY_GAME 권한 여부: {}", hasPlayGame);
    }

    @Test
    @DisplayName("아동별 주보호자 수 조회")
    void testCountPrimaryByChildId() {
        ChildrenAuthorizedUser primaryAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .isActive(true)
                .build();
        child.addAuthorizedUser(primaryAuth);
        authorizedUserRepository.save(primaryAuth);

        long primaryCount = authorizedUserRepository.countPrimaryByChildId(child.getChildId());

        assertThat(primaryCount).isEqualTo(1);
        log.info("주보호자 수: {}", primaryCount);
    }

    @Test
    @DisplayName("권한 추가 및 제거")
    void testAddAndRemovePermission() {
        ChildrenAuthorizedUser authorization = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(true)
                .build();
        child.addAuthorizedUser(authorization);
        authorizedUserRepository.save(authorization);

        authorization.addPermission(ChildPermissionType.WRITE_NOTE);
        authorizedUserRepository.save(authorization);

        ChildrenAuthorizedUser found = authorizedUserRepository
                .findByChildIdAndUserId(child.getChildId(), therapist.getUserId())
                .orElseThrow();

        assertThat(found.getPermissions()).hasSize(2);
        assertThat(found.hasPermission(ChildPermissionType.WRITE_NOTE)).isTrue();

        found.removePermission(ChildPermissionType.VIEW_REPORT);
        authorizedUserRepository.save(found);

        ChildrenAuthorizedUser updated = authorizedUserRepository
                .findByChildIdAndUserId(child.getChildId(), therapist.getUserId())
                .orElseThrow();

        assertThat(updated.getPermissions()).hasSize(1);
        assertThat(updated.hasPermission(ChildPermissionType.VIEW_REPORT)).isFalse();
        assertThat(updated.hasPermission(ChildPermissionType.WRITE_NOTE)).isTrue();

        log.info("권한 추가/제거 후 상태: {}", updated.getPermissions());
    }

    @Test
    @DisplayName("주보호자는 모든 권한 보유")
    void testPrimaryParentHasAllPermissions() {
        ChildrenAuthorizedUser primaryAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of())
                .isActive(true)
                .build();
        child.addAuthorizedUser(primaryAuth);
        authorizedUserRepository.save(primaryAuth);

        assertThat(primaryAuth.hasPermission(ChildPermissionType.PLAY_GAME)).isTrue();
        assertThat(primaryAuth.hasPermission(ChildPermissionType.VIEW_REPORT)).isTrue();
        assertThat(primaryAuth.hasPermission(ChildPermissionType.WRITE_NOTE)).isTrue();
        assertThat(primaryAuth.hasPermission(ChildPermissionType.ASSIGN_MISSION)).isTrue();
        assertThat(primaryAuth.hasPermission(ChildPermissionType.MANAGE)).isTrue();

        log.info("주보호자 전체 권한 검증 완료");
    }

    @Test
    @DisplayName("주보호자 권한은 제거 불가")
    void testPrimaryParentCannotRemovePermission() {
        ChildrenAuthorizedUser primaryAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .isActive(true)
                .build();
        child.addAuthorizedUser(primaryAuth);
        authorizedUserRepository.save(primaryAuth);

        assertThatThrownBy(() -> primaryAuth.removePermission(ChildPermissionType.PLAY_GAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("주보호자의 권한은 제거할 수 없습니다.");

        assertThatThrownBy(primaryAuth::clearPermissions)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("주보호자의 권한은 제거할 수 없습니다.");

        log.info("주보호자 권한 제거 불가 검증 완료");
    }

    @Test
    @DisplayName("비활성 권한은 활성 조회에서 제외")
    void testInactiveAuthorizationNotFound() {
        ChildrenAuthorizedUser authorization = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(false)
                .build();
        child.addAuthorizedUser(authorization);
        authorizedUserRepository.save(authorization);

        List<ChildrenAuthorizedUser> activeAuths = authorizedUserRepository
                .findActiveByChildId(child.getChildId());

        assertThat(activeAuths).isEmpty();
        log.info("비활성 권한 제외 검증 완료");
    }
}