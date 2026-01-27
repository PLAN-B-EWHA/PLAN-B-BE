package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
@ActiveProfiles
@Slf4j
@DisplayName("Child Repository 테스트")
public class ChildRepositoryTest {


    @Autowired
    private ChildRepository childRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChildrenAuthorizedUserRepository authorizedUserRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("아동 생성 - 기본 정보만")
    void testInsertChild() {
        // Given
        Child child = Child.builder()
                .name("테스트아동")
                .birthDate(LocalDate.of(2020, 5, 15))
                .gender("MALE")
                .build();

        // When
        Child savedChild = childRepository.save(child);

        // Then
        assertThat(savedChild.getChildId()).isNotNull();
        assertThat(savedChild.getName()).isEqualTo("테스트아동");
        assertThat(savedChild.getBirthDate()).isEqualTo(LocalDate.of(2020, 5, 15));
        assertThat(savedChild.getGender()).isEqualTo("MALE");
        assertThat(savedChild.getIsDeleted()).isFalse();

        log.info("저장된 아동: {}", savedChild);
    }

    @Test
    @DisplayName("아동 생성 + 주보호자 권한 부여")
    @Transactional
    void testInsertChildWithPrimaryParent() {
        // Given - 부모 사용자 생성
        User parent = User.builder()
                .email("parent@test.com")
                .password(passwordEncoder.encode("password"))
                .name("부모님")
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(parent);

        // Given - 아동 생성
        Child child = Child.builder()
                .name("민수")
                .birthDate(LocalDate.of(2020, 3, 10))
                .gender("MALE")
                .build();
        childRepository.save(child);

        // Given - 주보호자 권한 부여
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
        authorizedUserRepository.save(authorization);
        childRepository.flush();

        // Then
        Child foundChild = childRepository.findByIdWithAuthorizedUsers(child.getChildId()).orElseThrow();

        assertThat(foundChild.getAuthorizedUsers()).hasSize(1);
        assertThat(foundChild.getPrimaryParent()).isPresent();
        assertThat(foundChild.getPrimaryParent().get().getEmail()).isEqualTo("parent@test.com");
        assertThat(foundChild.isPrimaryParent(parent.getUserId())).isTrue();

        log.info("주보호자와 함께 저장된 아동: {}", foundChild);
        log.info("주보호자: {}", foundChild.getPrimaryParent().get());
    }

    @Test
    @DisplayName("치료사는 주보호자 불가 - PARENT만 가능")
    void testTherapistCannotBePrimary() {
        // Given - 치료사 사용자
        User therapist = User.builder()
                .email("therapist@test.com")
                .password(passwordEncoder.encode("password"))
                .name("치료사")
                .roles(Set.of(UserRole.THERAPIST))
                .build();
        userRepository.save(therapist);

        // Given - 아동
        Child child = Child.builder()
                .name("철수")
                .build();
        childRepository.save(child);

        // When & Then - 치료사를 주보호자로 설정 시도
        ChildrenAuthorizedUser auth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(true)
                .build();

        assertThatThrownBy(() -> child.addAuthorizedUser(auth))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("주보호자는 PARENT 역할만 가능합니다");

        log.info("치료사 주보호자 제약 조건 정상 작동");
    }

    @Test
    @DisplayName("Soft Delete - 삭제된 아동은 조회 안 됨")
    void testSoftDelete() {
        // Given
        Child child = Child.builder()
                .name("삭제될아동")
                .build();
        childRepository.save(child);

        // When - Soft Delete
        child.delete();
        childRepository.save(child);
        childRepository.flush();

        // Then - 일반 조회 시 안 보임 (@SQLRestriction)
        Optional<Child> found = childRepository.findById(child.getChildId());

        assertThat(found).isEmpty();

        log.info("Soft Delete 정상 작동 - 삭제된 아동 조회 안 됨");
    }

    @Test
    @DisplayName("주보호자로 아동 목록 조회")
    @Transactional
    void testFindByPrimaryParent() {
        // Given - 부모 1명, 아동 2명
        User parent = User.builder()
                .email("parent@test.com")
                .password(passwordEncoder.encode("password"))
                .name("부모")
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(parent);

        Child child1 = Child.builder().name("첫째").build();
        Child child2 = Child.builder().name("둘째").build();
        childRepository.saveAll(List.of(child1, child2));

        // 주보호자 권한 부여
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
        childRepository.flush();

        // When
        List<Child> children = childRepository.findByPrimaryParentUserId(parent.getUserId());

        // Then
        assertThat(children).hasSize(2);
        assertThat(children).extracting(Child::getName)
                .containsExactlyInAnyOrder("첫째", "둘째");

        log.info("주보호자의 아동 목록: {}", children);
    }

    @Test
    @DisplayName("PIN 설정 및 검증")
    void testPinCode() {
        // Given
        Child child = Child.builder()
                .name("PIN테스트")
                .build();
        childRepository.save(child);

        String rawPin = "1234";
        String encryptedPin = passwordEncoder.encode(rawPin);

        // When - PIN 설정
        child.setPinCode(encryptedPin);
        childRepository.save(child);

        // Then
        Child found = childRepository.findById(child.getChildId()).orElseThrow();

        assertThat(found.getPinEnabled()).isTrue();
        assertThat(found.getPinCode()).isNotNull();
        assertThat(found.verifyPin("1234", passwordEncoder)).isTrue();
        assertThat(found.verifyPin("9999", passwordEncoder)).isFalse();

        log.info("PIN 설정 및 검증 성공");
    }


    @Test
    @DisplayName("권한별 필터링 조회")
    @Transactional
    void testFindAccessibleChildren() {
        // Given - 부모, 치료사 생성
        User parent = User.builder()
                .email("parent@test.com")
                .password(passwordEncoder.encode("password"))
                .name("부모")
                .roles(Set.of(UserRole.PARENT))
                .build();

        User therapist = User.builder()
                .email("therapist@test.com")
                .password(passwordEncoder.encode("password"))
                .name("치료사")
                .roles(Set.of(UserRole.THERAPIST))
                .build();

        userRepository.saveAll(List.of(parent, therapist));

        // Given - 아동 생성
        Child child = Child.builder().name("권한테스트").build();
        childRepository.save(child);

        // 부모: 주보호자
        ChildrenAuthorizedUser parentAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .isActive(true)
                .build();
        child.addAuthorizedUser(parentAuth);

        // 치료사: VIEW_REPORT만
        ChildrenAuthorizedUser therapistAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(false)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(true)
                .build();
        child.addAuthorizedUser(therapistAuth);

        authorizedUserRepository.saveAll(List.of(parentAuth, therapistAuth));
        childRepository.flush();

        // When
        List<Child> parentChildren = childRepository.findAccessibleByUserId(parent.getUserId());
        List<Child> therapistChildren = childRepository.findAccessibleByUserId(therapist.getUserId());

        // Then
        assertThat(parentChildren).hasSize(1);
        assertThat(therapistChildren).hasSize(1);

        Child parentChild = parentChildren.get(0);
        assertThat(parentChild.hasPermission(parent.getUserId(), ChildPermissionType.PLAY_GAME)).isTrue();

        Child therapistChild = therapistChildren.get(0);
        assertThat(therapistChild.hasPermission(therapist.getUserId(), ChildPermissionType.VIEW_REPORT)).isTrue();
        assertThat(therapistChild.hasPermission(therapist.getUserId(), ChildPermissionType.PLAY_GAME)).isFalse();

        log.info("부모 접근 가능 아동: {}", parentChildren);
        log.info("치료사 접근 가능 아동: {}", therapistChildren);
    }



}
