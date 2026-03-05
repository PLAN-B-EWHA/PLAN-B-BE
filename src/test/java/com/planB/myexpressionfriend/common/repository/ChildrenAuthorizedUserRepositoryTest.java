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

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Slf4j
@DisplayName("ChildrenAuthorizedUser Repository ?뚯뒪??)
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
        //怨듯넻 ?뚯뒪???곗씠??
        parent = User.builder()
                .email("parent@test.com")
                .password(passwordEncoder.encode("password"))
                .name("遺紐?)
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(parent);

        therapist = User.builder()
                .email("therapist@test.com")
                .password(passwordEncoder.encode("password"))
                .name("移섎즺??)
                .roles(Set.of(UserRole.THERAPIST))
                .build();
        userRepository.save(therapist);

        child = Child.builder()
                .name("?뚯뒪???꾨룞")
                .birthDate(LocalDate.of(2020,5,15))
                .gender("MALE")
                .build();
        childRepository.save(child);
    }


    @Test
    @DisplayName("沅뚰븳 ?앹꽦 - 二쇰낫?몄옄")
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

        log.info("??λ맂 二쇰낫?몄옄 沅뚰븳: {}", saved);
    }

    @Test
    @DisplayName("沅뚰븳 ?앹꽦 - 移섎즺???쒗븳??沅뚰븳)")
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

        log.info("??λ맂 移섎즺??沅뚰븳: {}", saved);
    }


    @Test
    @DisplayName("?꾨룞 + ?ъ슜?먮줈 沅뚰븳 議고쉶")
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

        log.info("議고쉶??沅뚰븳: {}", found.get());
    }

    @Test
    @DisplayName("?꾨룞 ID + ?ъ슜??ID濡?沅뚰븳 議고쉶")
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

        log.info("ID濡?議고쉶??沅뚰븳: {}", found.get());
    }

    @Test
    @DisplayName("?뱀젙 ?꾨룞??활성화된 권한 목록 조회")
    public void testFindActiveByChildId() {

        // Given - 遺紐?(二쇰낫?몄옄)
        ChildrenAuthorizedUser parentAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .isActive(true)
                .build();
        child.addAuthorizedUser(parentAuth);

        // Given - 移섎즺??(?쒖꽦??
        ChildrenAuthorizedUser therapistAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(true)
                .build();
        child.addAuthorizedUser(therapistAuth);

        // Given - 鍮꾪솢?깊솕???ъ슜??
        User inactiveUser = User.builder()
                .email("inactive@test.com")
                .password(passwordEncoder.encode("password"))
                .name("鍮꾪솢??)
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(inactiveUser);

        ChildrenAuthorizedUser inactiveAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(inactiveUser)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(false)  // 鍮꾪솢?깊솕
                .build();
        child.addAuthorizedUser(inactiveAuth);

        authorizedUserRepository.saveAll(List.of(parentAuth, therapistAuth, inactiveAuth));

        // When
        List<ChildrenAuthorizedUser> activeAuths = authorizedUserRepository.findActiveByChildId(child.getChildId());

        // Then
        assertThat(activeAuths).hasSize(2);  // ?쒖꽦?붾맂 寃껊쭔
        assertThat(activeAuths).extracting(au -> au.getUser().getEmail())
                .containsExactlyInAnyOrder("parent@test.com", "therapist@test.com");

        // 二쇰낫?몄옄媛 泥?踰덉㎏濡??뺣젹?섏뼱????
        assertThat(activeAuths.get(0).getIsPrimary()).isTrue();

        log.info("?쒖꽦?붾맂 沅뚰븳 紐⑸줉: {}", activeAuths);
    }

    @Test
    @DisplayName("二쇰낫?몄옄 議고쉶")
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

        log.info("二쇰낫?몄옄: {}", primary.get());
    }

    @Test
    @DisplayName("?뱀젙 ?ъ슜?먭? 二쇰낫?몄옄???꾨룞 紐⑸줉 議고쉶")
    void testFindPrimaryByUserId() {

        // Given - ?꾨룞 2紐??앹꽦
        Child child1 = Child.builder().name("泥レ㎏").build();
        Child child2 = Child.builder().name("?섏㎏").build();
        childRepository.saveAll(List.of(child1, child2));

        // 泥レ㎏??二쇰낫?몄옄
        ChildrenAuthorizedUser auth1 = ChildrenAuthorizedUser.builder()
                .child(child1)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .isActive(true)
                .build();
        child1.addAuthorizedUser(auth1);

        // ?섏㎏??二쇰낫?몄옄
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
                .containsExactlyInAnyOrder("泥レ㎏", "?섏㎏");

        log.info("遺紐④? 二쇰낫?몄옄???꾨룞?? {}", primaryAuths);
    }

    @Test
    @DisplayName("?뱀젙 沅뚰븳??媛吏??ъ슜??紐⑸줉 議고쉶")
    void testFindByChildIdAndPermission() {

        // Given - ?щ윭 ?ъ슜?먯뿉寃??ㅼ뼇??沅뚰븳 遺??
        User user1 = User.builder()
                .email("user1@test.com")
                .password(passwordEncoder.encode("password"))
                .name("?ъ슜??")
                .roles(Set.of(UserRole.PARENT))
                .build();

        User user2 = User.builder()
                .email("user2@test.com")
                .password(passwordEncoder.encode("password"))
                .name("?ъ슜??")
                .roles(Set.of(UserRole.THERAPIST))
                .build();

        userRepository.saveAll(List.of(user1, user2));

        // user1: PLAY_GAME 沅뚰븳
        ChildrenAuthorizedUser auth1 = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(user1)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.PLAY_GAME))
                .isActive(true)
                .build();
        child.addAuthorizedUser(auth1);

        // user2: VIEW_REPORT留?
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

        log.info("PLAY_GAME 沅뚰븳 ?ъ슜?? {}", playGameUsers);
        log.info("VIEW_REPORT 沅뚰븳 ?ъ슜?? {}", viewReportUsers);
    }

    @Test
    @DisplayName("沅뚰븳 議댁옱 ?щ? ?뺤씤")
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

        log.info("VIEW_REPORT 沅뚰븳 議댁옱: {}", hasViewReport);
        log.info("PLAY_GAME 沅뚰븳 議댁옱: {}", hasPlayGame);
    }

    @Test
    @DisplayName("二쇰낫?몄옄 媛쒖닔 ?뺤씤 - 1紐??쒗븳")
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

        log.info("二쇰낫?몄옄 媛쒖닔: {}", primaryCount);
    }

    @Test
    @DisplayName("沅뚰븳 異붽? 諛??쒓굅")
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

        // When - 沅뚰븳 異붽?
        authorization.addPermission(ChildPermissionType.WRITE_NOTE);
        authorizedUserRepository.save(authorization);

        // Then - 異붽? ?뺤씤
        ChildrenAuthorizedUser found = authorizedUserRepository
                .findByChildIdAndUserId(child.getChildId(), therapist.getUserId())
                .orElseThrow();

        assertThat(found.getPermissions()).hasSize(2);
        assertThat(found.hasPermission(ChildPermissionType.WRITE_NOTE)).isTrue();

        // When - 沅뚰븳 ?쒓굅
        found.removePermission(ChildPermissionType.VIEW_REPORT);
        authorizedUserRepository.save(found);

        // Then - ?쒓굅 ?뺤씤
        ChildrenAuthorizedUser updated = authorizedUserRepository
                .findByChildIdAndUserId(child.getChildId(), therapist.getUserId())
                .orElseThrow();

        assertThat(updated.getPermissions()).hasSize(1);
        assertThat(updated.hasPermission(ChildPermissionType.VIEW_REPORT)).isFalse();
        assertThat(updated.hasPermission(ChildPermissionType.WRITE_NOTE)).isTrue();

        log.info("沅뚰븳 異붽?/?쒓굅 ?깃났: {}", updated.getPermissions());
    }

    @Test
    @DisplayName("二쇰낫?몄옄??紐⑤뱺 沅뚰븳 ?먮룞 蹂댁쑀")
    void testPrimaryParentHasAllPermissions() {
        // Given
        ChildrenAuthorizedUser primaryAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of())  // 鍮?沅뚰븳?쇰줈 ?쒖옉
                .isActive(true)
                .build();
        child.addAuthorizedUser(primaryAuth);
        authorizedUserRepository.save(primaryAuth);

        // When & Then - isPrimary=true硫?紐⑤뱺 沅뚰븳 ?먮룞 蹂댁쑀
        assertThat(primaryAuth.hasPermission(ChildPermissionType.PLAY_GAME)).isTrue();
        assertThat(primaryAuth.hasPermission(ChildPermissionType.VIEW_REPORT)).isTrue();
        assertThat(primaryAuth.hasPermission(ChildPermissionType.WRITE_NOTE)).isTrue();
        assertThat(primaryAuth.hasPermission(ChildPermissionType.ASSIGN_MISSION)).isTrue();
        assertThat(primaryAuth.hasPermission(ChildPermissionType.MANAGE)).isTrue();

        log.info("二쇰낫?몄옄??紐⑤뱺 沅뚰븳 ?먮룞 蹂댁쑀 ?뺤씤");
    }

    @Test
    @DisplayName("二쇰낫?몄옄??沅뚰븳 ?쒓굅 遺덇?")
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

        // When & Then - 二쇰낫?몄옄 沅뚰븳 ?쒓굅 ?쒕룄
        assertThatThrownBy(() -> primaryAuth.removePermission(ChildPermissionType.PLAY_GAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("주보호자 권한은 제거할 수 없습니다.");

        assertThatThrownBy(() -> primaryAuth.clearPermissions())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("주보호자 권한은 제거할 수 없습니다.");

        log.info("二쇰낫?몄옄 沅뚰븳 ?쒓굅 諛⑹? ?뺤긽 ?묐룞");
    }

    @Test
    @DisplayName("鍮꾪솢?깊솕??沅뚰븳? 議고쉶 ????)
    void testInactiveAuthorizationNotFound() {
        // Given
        ChildrenAuthorizedUser authorization = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(false)  // 鍮꾪솢?깊솕
                .build();
        child.addAuthorizedUser(authorization);
        authorizedUserRepository.save(authorization);

        // When
        List<ChildrenAuthorizedUser> activeAuths = authorizedUserRepository
                .findActiveByChildId(child.getChildId());

        // Then
        assertThat(activeAuths).isEmpty();

        log.info("鍮꾪솢?깊솕??沅뚰븳 ?꾪꽣留??뺤긽 ?묐룞");
    }
}
