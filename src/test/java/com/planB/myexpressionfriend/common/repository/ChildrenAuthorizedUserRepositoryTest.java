package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

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
        //공통 테스트 데이터
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
                .name("테스트 아동")
                .birthDate(LocalDate.of(2020,5,15))
                .gender("MALE")
                .build();
        childRepository.save(child);
    }


    @Test
    @DisplayName("권한 생성 - 주보호자")
    void testInsertPrimaryAuthorization() {

        // Given
        ChildrenAuthorizedUser authorization = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .authorizedBy(parent)
                .isActive(true)
                .build();

        // When
        child.addAuthorizedUser(authorization);
        ChildrenAuthorizedUser saved = authorizedUserRepository.save(authorization);

        // Then
        assertThat(saved.getAuthorizationId()).isNotNull();
        assertThat(saved.getIsPrimary()).isTrue();
        assertThat(saved.getIsActive()).isTrue();
        assertThat(saved.getPermissions()).containsExactlyInAnyOrder(ChildPermissionType.values());
        assertThat(saved.hasPermission(ChildPermissionType.PLAY_GAME)).isTrue();
        assertThat(saved.hasPermission(ChildPermissionType.VIEW_REPORT)).isTrue();

        log.info("저장된 주보호자 권한: {}", saved);
    }

    @Test
    @DisplayName("권한 생성 - 치료사(제한된 권한)")
    void testInsertTherapistAuthorization() {

        // Given
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

        // When
        child.addAuthorizedUser(authorization);
        ChildrenAuthorizedUser saved = authorizedUserRepository.save(authorization);

        // Then
        assertThat(saved.getIsPrimary()).isFalse();
        assertThat(saved.getPermissions()).hasSize(2);
        assertThat(saved.hasPermission(ChildPermissionType.VIEW_REPORT)).isTrue();
        assertThat(saved.hasPermission(ChildPermissionType.WRITE_NOTE)).isTrue();
        assertThat(saved.hasPermission(ChildPermissionType.PLAY_GAME)).isFalse();

        log.info("저장된 치료사 권한: {}", saved);
    }


    @Test
    @DisplayName("아동 + 사용자로 권한 조회")
    void testFindByChildAndUser() {

        // Given
        ChildrenAuthorizedUser authorization = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .isActive(true)
                .build();

        child.addAuthorizedUser(authorization);
        authorizedUserRepository.save(authorization);

        // When
        Optional<ChildrenAuthorizedUser> found = authorizedUserRepository.findByChildAndUser(child, parent);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getIsPrimary()).isTrue();
        assertThat(found.get().getUser().getEmail()).isEqualTo("parent@test.com");

        log.info("조회된 권한: {}", found.get());
    }

    @Test
    @DisplayName("아동 ID + 사용자 ID로 권한 조회")
    void testFindByChildIdAndUserId() {

        // Given
        ChildrenAuthorizedUser authorization = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(true)
                .build();
        child.addAuthorizedUser(authorization);
        authorizedUserRepository.save(authorization);

        // When
        Optional<ChildrenAuthorizedUser> found = authorizedUserRepository
                .findByChildIdAndUserId(child.getChildId(), therapist.getUserId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getUserId()).isEqualTo(therapist.getUserId());
        assertThat(found.get().hasPermission(ChildPermissionType.VIEW_REPORT)).isTrue();

        log.info("ID로 조회된 권한: {}", found.get());
    }

    @Test
    @DisplayName("특정 아동의 활성화된 권한 목록 조회")
    public void testFindActiveByChildId() {

        // Given - 부모 (주보호자)
        ChildrenAuthorizedUser parentAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .isActive(true)
                .build();
        child.addAuthorizedUser(parentAuth);

        // Given - 치료사 (활성화)
        ChildrenAuthorizedUser therapistAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(true)
                .build();
        child.addAuthorizedUser(therapistAuth);

        // Given - 비활성화된 사용자
        User inactiveUser = User.builder()
                .email("inactive@test.com")
                .password(passwordEncoder.encode("password"))
                .name("비활성")
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(inactiveUser);

        ChildrenAuthorizedUser inactiveAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(inactiveUser)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(false)  // 비활성화
                .build();
        child.addAuthorizedUser(inactiveAuth);

        authorizedUserRepository.saveAll(List.of(parentAuth, therapistAuth, inactiveAuth));

        // When
        List<ChildrenAuthorizedUser> activeAuths = authorizedUserRepository.findActiveByChildId(child.getChildId());

        // Then
        assertThat(activeAuths).hasSize(2);  // 활성화된 것만
        assertThat(activeAuths).extracting(au -> au.getUser().getEmail())
                .containsExactlyInAnyOrder("parent@test.com", "therapist@test.com");

        // 주보호자가 첫 번째로 정렬되어야 함
        assertThat(activeAuths.get(0).getIsPrimary()).isTrue();

        log.info("활성화된 권한 목록: {}", activeAuths);
    }

    @Test
    @DisplayName("주보호자 조회")
    public void testFindPrimaryByChildId() {

        // Given
        ChildrenAuthorizedUser primaryAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .isActive(true)
                .build();
        child.addAuthorizedUser(primaryAuth);
        authorizedUserRepository.save(primaryAuth);

        // When
        Optional<ChildrenAuthorizedUser> primary = authorizedUserRepository
                .findPrimaryByChildId(child.getChildId());

        // Then
        assertThat(primary).isPresent();
        assertThat(primary.get().getIsPrimary()).isTrue();
        assertThat(primary.get().getUser().getEmail()).isEqualTo("parent@test.com");

        log.info("주보호자: {}", primary.get());
    }

    @Test
    @DisplayName("특정 사용자가 주보호자인 아동 목록 조회")
    void testFindPrimaryByUserId() {

        // Given - 아동 2명 생성
        Child child1 = Child.builder().name("첫째").build();
        Child child2 = Child.builder().name("둘째").build();
        childRepository.saveAll(List.of(child1, child2));

        // 첫째의 주보호자
        ChildrenAuthorizedUser auth1 = ChildrenAuthorizedUser.builder()
                .child(child1)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .isActive(true)
                .build();
        child1.addAuthorizedUser(auth1);

        // 둘째의 주보호자
        ChildrenAuthorizedUser auth2 = ChildrenAuthorizedUser.builder()
                .child(child2)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .isActive(true)
                .build();
        child2.addAuthorizedUser(auth2);

        authorizedUserRepository.saveAll(List.of(auth1, auth2));

        // When
        List<ChildrenAuthorizedUser> primaryAuths = authorizedUserRepository
                .findPrimaryByUserId(parent.getUserId());

        // Then
        assertThat(primaryAuths).hasSize(2);
        assertThat(primaryAuths).allMatch(au -> au.getIsPrimary());
        assertThat(primaryAuths).extracting(au -> au.getChild().getName())
                .containsExactlyInAnyOrder("첫째", "둘째");

        log.info("부모가 주보호자인 아동들: {}", primaryAuths);
    }

    @Test
    @DisplayName("특정 권한을 가진 사용자 목록 조회")
    void testFindByChildIdAndPermission() {

        // Given - 여러 사용자에게 다양한 권한 부여
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

        // user1: PLAY_GAME 권한
        ChildrenAuthorizedUser auth1 = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(user1)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.PLAY_GAME))
                .isActive(true)
                .build();
        child.addAuthorizedUser(auth1);

        // user2: VIEW_REPORT만
        ChildrenAuthorizedUser auth2 = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(user2)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(true)
                .build();
        child.addAuthorizedUser(auth2);

        authorizedUserRepository.saveAll(List.of(auth1, auth2));

        // When
        List<ChildrenAuthorizedUser> playGameUsers = authorizedUserRepository
                .findByChildIdAndPermission(child.getChildId(), ChildPermissionType.PLAY_GAME);

        List<ChildrenAuthorizedUser> viewReportUsers = authorizedUserRepository
                .findByChildIdAndPermission(child.getChildId(), ChildPermissionType.VIEW_REPORT);

        // Then
        assertThat(playGameUsers).hasSize(1);
        assertThat(playGameUsers.get(0).getUser().getEmail()).isEqualTo("user1@test.com");

        assertThat(viewReportUsers).hasSize(1);
        assertThat(viewReportUsers.get(0).getUser().getEmail()).isEqualTo("user2@test.com");

        log.info("PLAY_GAME 권한 사용자: {}", playGameUsers);
        log.info("VIEW_REPORT 권한 사용자: {}", viewReportUsers);
    }

    @Test
    @DisplayName("권한 존재 여부 확인")
    void testExistsByChildIdAndUserIdAndPermission() {
        // Given
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

        // When & Then
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

        log.info("VIEW_REPORT 권한 존재: {}", hasViewReport);
        log.info("PLAY_GAME 권한 존재: {}", hasPlayGame);
    }

    @Test
    @DisplayName("주보호자 개수 확인 - 1명 제한")
    void testCountPrimaryByChildId() {
        // Given
        ChildrenAuthorizedUser primaryAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .isActive(true)
                .build();
        child.addAuthorizedUser(primaryAuth);
        authorizedUserRepository.save(primaryAuth);

        // When
        long primaryCount = authorizedUserRepository.countPrimaryByChildId(child.getChildId());

        // Then
        assertThat(primaryCount).isEqualTo(1);

        log.info("주보호자 개수: {}", primaryCount);
    }

    @Test
    @DisplayName("권한 추가 및 제거")
    void testAddAndRemovePermission() {
        // Given
        ChildrenAuthorizedUser authorization = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(true)
                .build();
        child.addAuthorizedUser(authorization);
        authorizedUserRepository.save(authorization);

        // When - 권한 추가
        authorization.addPermission(ChildPermissionType.WRITE_NOTE);
        authorizedUserRepository.save(authorization);

        // Then - 추가 확인
        ChildrenAuthorizedUser found = authorizedUserRepository
                .findByChildIdAndUserId(child.getChildId(), therapist.getUserId())
                .orElseThrow();

        assertThat(found.getPermissions()).hasSize(2);
        assertThat(found.hasPermission(ChildPermissionType.WRITE_NOTE)).isTrue();

        // When - 권한 제거
        found.removePermission(ChildPermissionType.VIEW_REPORT);
        authorizedUserRepository.save(found);

        // Then - 제거 확인
        ChildrenAuthorizedUser updated = authorizedUserRepository
                .findByChildIdAndUserId(child.getChildId(), therapist.getUserId())
                .orElseThrow();

        assertThat(updated.getPermissions()).hasSize(1);
        assertThat(updated.hasPermission(ChildPermissionType.VIEW_REPORT)).isFalse();
        assertThat(updated.hasPermission(ChildPermissionType.WRITE_NOTE)).isTrue();

        log.info("권한 추가/제거 성공: {}", updated.getPermissions());
    }

    @Test
    @DisplayName("주보호자는 모든 권한 자동 보유")
    void testPrimaryParentHasAllPermissions() {
        // Given
        ChildrenAuthorizedUser primaryAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of())  // 빈 권한으로 시작
                .isActive(true)
                .build();
        child.addAuthorizedUser(primaryAuth);
        authorizedUserRepository.save(primaryAuth);

        // When & Then - isPrimary=true면 모든 권한 자동 보유
        assertThat(primaryAuth.hasPermission(ChildPermissionType.PLAY_GAME)).isTrue();
        assertThat(primaryAuth.hasPermission(ChildPermissionType.VIEW_REPORT)).isTrue();
        assertThat(primaryAuth.hasPermission(ChildPermissionType.WRITE_NOTE)).isTrue();
        assertThat(primaryAuth.hasPermission(ChildPermissionType.ASSIGN_MISSION)).isTrue();
        assertThat(primaryAuth.hasPermission(ChildPermissionType.MANAGE)).isTrue();

        log.info("주보호자는 모든 권한 자동 보유 확인");
    }

    @Test
    @DisplayName("주보호자는 권한 제거 불가")
    void testPrimaryParentCannotRemovePermission() {
        // Given
        ChildrenAuthorizedUser primaryAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .isActive(true)
                .build();
        child.addAuthorizedUser(primaryAuth);
        authorizedUserRepository.save(primaryAuth);

        // When & Then - 주보호자 권한 제거 시도
        assertThatThrownBy(() -> primaryAuth.removePermission(ChildPermissionType.PLAY_GAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("주보호자의 권한은 제거할 수 없습니다");

        assertThatThrownBy(() -> primaryAuth.clearPermissions())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("주보호자의 권한은 제거할 수 없습니다");

        log.info("주보호자 권한 제거 방지 정상 작동");
    }

    @Test
    @DisplayName("비활성화된 권한은 조회 안 됨")
    void testInactiveAuthorizationNotFound() {
        // Given
        ChildrenAuthorizedUser authorization = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(false)  // 비활성화
                .build();
        child.addAuthorizedUser(authorization);
        authorizedUserRepository.save(authorization);

        // When
        List<ChildrenAuthorizedUser> activeAuths = authorizedUserRepository
                .findActiveByChildId(child.getChildId());

        // Then
        assertThat(activeAuths).isEmpty();

        log.info("비활성화된 권한 필터링 정상 작동");
    }
}
