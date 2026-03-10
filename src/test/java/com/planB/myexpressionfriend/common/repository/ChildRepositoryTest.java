package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.child.ChildrenAuthorizedUser;
import com.planB.myexpressionfriend.common.domain.child.ExpressionTag;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import lombok.extern.slf4j.Slf4j;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("아동 생성 - 기본 정보")
    void testInsertChild() {
        Child child = Child.builder()
                .name("테스트아동")
                .birthDate(LocalDate.of(2020, 5, 15))
                .gender("MALE")
                .build();

        Child savedChild = childRepository.save(child);

        assertThat(savedChild.getChildId()).isNotNull();
        assertThat(savedChild.getBirthDate()).isEqualTo(LocalDate.of(2020, 5, 15));
        assertThat(savedChild.getGender()).isEqualTo("MALE");
        assertThat(savedChild.getIsDeleted()).isFalse();

        log.info("아동 저장 완료: {}", savedChild);
    }

    @Test
    @DisplayName("아동 생성과 주보호자 연결")
    @Transactional
    void testInsertChildWithPrimaryParent() {
        User parent = User.builder()
                .email("parent@test.com")
                .password(passwordEncoder.encode("password"))
                .name("부모")
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(parent);

        Child child = Child.builder()
                .name("테스트아동")
                .birthDate(LocalDate.of(2020, 3, 10))
                .gender("MALE")
                .build();
        childRepository.save(child);

        ChildrenAuthorizedUser authorization = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .authorizedBy(parent)
                .isActive(true)
                .build();

        child.addAuthorizedUser(authorization);
        authorizedUserRepository.save(authorization);
        childRepository.flush();

        Child foundChild = childRepository.findByIdWithAuthorizedUsers(child.getChildId()).orElseThrow();

        assertThat(foundChild.getAuthorizedUsers()).hasSize(1);
        assertThat(foundChild.getPrimaryParent()).isPresent();
        assertThat(foundChild.getPrimaryParent().get().getEmail()).isEqualTo("parent@test.com");
        assertThat(foundChild.isPrimaryParent(parent.getUserId())).isTrue();

        log.info("주보호자 연결 아동 조회 결과: {}", foundChild);
        log.info("주보호자 정보: {}", foundChild.getPrimaryParent().get());
    }

    @Test
    @DisplayName("치료사는 주보호자가 될 수 없다")
    void testTherapistCannotBePrimary() {
        User therapist = User.builder()
                .email("therapist@test.com")
                .password(passwordEncoder.encode("password"))
                .name("치료사")
                .roles(Set.of(UserRole.THERAPIST))
                .build();
        userRepository.save(therapist);

        Child child = Child.builder()
                .name("테스트아동")
                .build();
        childRepository.save(child);

        ChildrenAuthorizedUser auth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(true)
                .build();

        assertThatThrownBy(() -> child.addAuthorizedUser(auth))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PARENT 역할을 가진 사용자만 주보호자가 될 수 있습니다.");

        log.info("치료사 주보호자 제한 검증 완료");
    }

    @Test
    @DisplayName("Soft Delete 후 조회되지 않음")
    void testSoftDelete() {
        Child child = Child.builder()
                .name("삭제아동")
                .build();
        childRepository.save(child);

        child.delete();
        childRepository.save(child);
        childRepository.flush();

        Optional<Child> found = childRepository.findById(child.getChildId());

        assertThat(found).isEmpty();
        log.info("Soft Delete 검증 완료");
    }

    @Test
    @DisplayName("주보호자 기준 아동 조회")
    @Transactional
    void testFindByPrimaryParent() {
        User parent = User.builder()
                .email("parent@test.com")
                .password(passwordEncoder.encode("password"))
                .name("학부모")
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(parent);

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
        childRepository.flush();

        List<Child> children = childRepository.findByPrimaryParentUserId(parent.getUserId());

        assertThat(children).hasSize(2);
        assertThat(children).extracting(Child::getName)
                .containsExactlyInAnyOrder("첫째", "둘째");

        log.info("주보호자 기준 조회 결과: {}", children);
    }

    @Test
    @DisplayName("PIN 코드 저장 및 검증")
    void testPinCode() {
        Child child = Child.builder()
                .name("핀아동")
                .build();
        childRepository.save(child);

        String rawPin = "1234";
        String encryptedPin = passwordEncoder.encode(rawPin);

        child.setPinCode(encryptedPin);
        childRepository.save(child);

        Child found = childRepository.findById(child.getChildId()).orElseThrow();

        assertThat(found.getPinEnabled()).isTrue();
        assertThat(found.getPinCode()).isNotNull();
        assertThat(found.verifyPin("1234", passwordEncoder)).isTrue();
        assertThat(found.verifyPin("9999", passwordEncoder)).isFalse();

        log.info("PIN 저장 및 검증 완료");
    }

    @Test
    @DisplayName("접근 가능한 아동 조회")
    @Transactional
    void testFindAccessibleChildren() {
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

        Child child = Child.builder().name("권한대상아동").build();
        childRepository.save(child);

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

        authorizedUserRepository.saveAll(List.of(parentAuth, therapistAuth));
        childRepository.flush();

        List<Child> parentChildren = childRepository.findAccessibleByUserId(parent.getUserId());
        List<Child> therapistChildren = childRepository.findAccessibleByUserId(therapist.getUserId());

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

    @Test
    @DisplayName("진단 정보와 특이사항 저장")
    void testInsertChildWithDiagnosisInfo() {
        Child child = Child.builder()
                .name("기록아동")
                .birthDate(LocalDate.of(2018, 7, 20))
                .diagnosisDate(LocalDate.of(2021, 3, 15))
                .diagnosisInfo("언어 발달 지연")
                .specialNotes("새로운 환경에 적응할 때 시간이 조금 더 필요함")
                .build();

        Child saved = childRepository.save(child);

        Child found = childRepository.findById(saved.getChildId()).orElseThrow();
        assertThat(found.getDiagnosisInfo()).isEqualTo("언어 발달 지연");
        assertThat(found.getSpecialNotes()).isEqualTo("새로운 환경에 적응할 때 시간이 조금 더 필요함");
        assertThat(found.getDiagnosisDate()).isEqualTo(LocalDate.of(2021, 3, 15));

        log.info("진단 정보 저장 완료: {}", found);
    }

    @Test
    @DisplayName("진단 정보는 3000자를 초과할 수 없음")
    void testChangeDiagnosisInfoTooLong() {
        Child child = Child.builder().name("진단아동").build();
        childRepository.save(child);

        String tooLong = "A".repeat(3001);

        assertThatThrownBy(() -> child.changeDiagnosisInfo(tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3000");
    }

    @Test
    @DisplayName("특이사항은 3000자를 초과할 수 없음")
    void testChangeSpecialNotesTooLong() {
        Child child = Child.builder().name("메모아동").build();
        childRepository.save(child);

        String tooLong = "B".repeat(3001);

        assertThatThrownBy(() -> child.changeSpecialNotes(tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3000");
    }

    @Test
    @DisplayName("선호 표현 저장")
    @Transactional
    void testPreferredExpressions() {
        Child child = Child.builder()
                .name("표현아동")
                .build();
        child.updatePreferredExpressions(Set.of(ExpressionTag.JOY, ExpressionTag.SURPRISE));
        childRepository.save(child);
        childRepository.flush();

        Child found = childRepository.findById(child.getChildId()).orElseThrow();

        assertThat(found.getPreferredExpressions())
                .containsExactlyInAnyOrder(ExpressionTag.JOY, ExpressionTag.SURPRISE);

        log.info("선호 표현 저장 결과: {}", found.getPreferredExpressions());
    }

    @Test
    @DisplayName("어려운 표현 저장")
    @Transactional
    void testDifficultExpressions() {
        Child child = Child.builder()
                .name("표현아동")
                .build();
        child.updateDifficultExpressions(Set.of(ExpressionTag.ANGRY, ExpressionTag.FEAR));
        childRepository.save(child);
        childRepository.flush();

        Child found = childRepository.findById(child.getChildId()).orElseThrow();

        assertThat(found.getDifficultExpressions())
                .containsExactlyInAnyOrder(ExpressionTag.ANGRY, ExpressionTag.FEAR);

        log.info("어려운 표현 저장 결과: {}", found.getDifficultExpressions());
    }

    @Test
    @DisplayName("선호 표현 재설정 시 기존 값 교체")
    @Transactional
    void testUpdatePreferredExpressionsReplaces() {
        Child child = Child.builder().name("표현교체아동").build();
        child.updatePreferredExpressions(Set.of(ExpressionTag.JOY, ExpressionTag.SAD));
        childRepository.save(child);
        childRepository.flush();

        child.updatePreferredExpressions(Set.of(ExpressionTag.NEUTRAL));
        childRepository.flush();

        Child found = childRepository.findById(child.getChildId()).orElseThrow();
        assertThat(found.getPreferredExpressions())
                .containsExactly(ExpressionTag.NEUTRAL)
                .doesNotContain(ExpressionTag.JOY, ExpressionTag.SAD);

        log.info("선호 표현 교체 결과: {}", found.getPreferredExpressions());
    }

    @Test
    @DisplayName("선호 표현 null 입력 시 비움")
    @Transactional
    void testUpdatePreferredExpressionsWithNull() {
        Child child = Child.builder().name("표현초기화아동").build();
        child.updatePreferredExpressions(Set.of(ExpressionTag.JOY));
        childRepository.save(child);
        childRepository.flush();

        child.updatePreferredExpressions(null);
        childRepository.flush();

        Child found = childRepository.findById(child.getChildId()).orElseThrow();
        assertThat(found.getPreferredExpressions()).isEmpty();
    }

    @Test
    @DisplayName("선호 표현으로 아동 조회")
    @Transactional
    void testFindByPreferredExpression() {
        Child child1 = Child.builder().name("기쁨아동").build();
        child1.updatePreferredExpressions(Set.of(ExpressionTag.JOY, ExpressionTag.SURPRISE));

        Child child2 = Child.builder().name("슬픔아동").build();
        child2.updatePreferredExpressions(Set.of(ExpressionTag.SAD));

        Child child3 = Child.builder().name("중립아동").build();
        child3.updatePreferredExpressions(Set.of(ExpressionTag.NEUTRAL));

        childRepository.saveAll(List.of(child1, child2, child3));
        childRepository.flush();

        List<Child> joyChildren = childRepository.findByPreferredExpression(ExpressionTag.JOY);

        assertThat(joyChildren).hasSize(1);
        assertThat(joyChildren.get(0).getName()).isEqualTo("기쁨아동");

        log.info("JOY 선호 아동: {}", joyChildren);
    }

    @Test
    @DisplayName("어려운 표현으로 아동 조회")
    @Transactional
    void testFindByDifficultExpression() {
        Child child1 = Child.builder().name("화남아동1").build();
        child1.updateDifficultExpressions(Set.of(ExpressionTag.ANGRY));

        Child child2 = Child.builder().name("화남아동2").build();
        child2.updateDifficultExpressions(Set.of(ExpressionTag.FEAR, ExpressionTag.ANGRY));

        Child child3 = Child.builder().name("놀람아동").build();
        child3.updateDifficultExpressions(Set.of(ExpressionTag.SURPRISE));

        childRepository.saveAll(List.of(child1, child2, child3));
        childRepository.flush();

        List<Child> angryChildren = childRepository.findByDifficultExpression(ExpressionTag.ANGRY);

        assertThat(angryChildren).hasSize(2);
        assertThat(angryChildren).extracting(Child::getName)
                .containsExactlyInAnyOrder("화남아동1", "화남아동2");

        log.info("ANGRY 어려움 아동: {}", angryChildren);
    }

    @Test
    @DisplayName("조회 결과가 없으면 빈 목록 반환")
    @Transactional
    void testFindByPreferredExpressionEmpty() {
        Child child = Child.builder().name("기쁨아동").build();
        child.updatePreferredExpressions(Set.of(ExpressionTag.JOY));
        childRepository.save(child);
        childRepository.flush();

        List<Child> result = childRepository.findByPreferredExpression(ExpressionTag.SAD);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("프로필 이미지 URL 변경")
    void testChangeProfileImageUrl() {
        Child child = Child.builder().name("프로필아동").build();
        childRepository.save(child);

        String imageUrl = "https://storage.example.com/profiles/child123.png";
        child.changeProfileImageUrl(imageUrl);
        childRepository.save(child);

        Child found = childRepository.findById(child.getChildId()).orElseThrow();
        assertThat(found.getProfileImageUrl()).isEqualTo(imageUrl);

        child.changeProfileImageUrl(null);
        childRepository.save(child);

        Child foundAfterRemove = childRepository.findById(child.getChildId()).orElseThrow();
        assertThat(foundAfterRemove.getProfileImageUrl()).isNull();

        log.info("프로필 이미지 URL 변경 검증 완료");
    }

    @Test
    @DisplayName("프로필 이미지 URL은 500자를 초과할 수 없음")
    void testChangeProfileImageUrlTooLong() {
        Child child = Child.builder().name("URL아동").build();
        childRepository.save(child);

        String tooLong = "https://example.com/" + "a".repeat(490);

        assertThatThrownBy(() -> child.changeProfileImageUrl(tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");
    }
}