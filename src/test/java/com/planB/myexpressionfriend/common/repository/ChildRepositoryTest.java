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

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles
@Slf4j
@DisplayName("Child Repository ?뚯뒪??)
public class ChildRepositoryTest {


    @Autowired
    private ChildRepository childRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChildrenAuthorizedUserRepository authorizedUserRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("?꾨룞 ?앹꽦 - 湲곕낯 ?뺣낫留?)
    void testInsertChild() {
        // Given
        Child child = Child.builder()
                .name("?뚯뒪?몄븘??)
                .birthDate(LocalDate.of(2020, 5, 15))
                .gender("MALE")
                .build();

        // When
        Child savedChild = childRepository.save(child);

        // Then
        assertThat(savedChild.getChildId()).isNotNull();
        assertThat(savedChild.getName()).isEqualTo("?뚯뒪?몄븘??);
        assertThat(savedChild.getBirthDate()).isEqualTo(LocalDate.of(2020, 5, 15));
        assertThat(savedChild.getGender()).isEqualTo("MALE");
        assertThat(savedChild.getIsDeleted()).isFalse();

        log.info("??λ맂 ?꾨룞: {}", savedChild);
    }

    @Test
    @DisplayName("?꾨룞 ?앹꽦 + 二쇰낫?몄옄 沅뚰븳 遺??)
    @Transactional
    void testInsertChildWithPrimaryParent() {
        // Given - 遺紐??ъ슜???앹꽦
        User parent = User.builder()
                .email("parent@test.com")
                .password(passwordEncoder.encode("password"))
                .name("遺紐⑤떂")
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(parent);

        // Given - ?꾨룞 ?앹꽦
        Child child = Child.builder()
                .name("誘쇱닔")
                .birthDate(LocalDate.of(2020, 3, 10))
                .gender("MALE")
                .build();
        childRepository.save(child);

        // Given - 二쇰낫?몄옄 沅뚰븳 遺??
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

        log.info("二쇰낫?몄옄? ?④퍡 ??λ맂 ?꾨룞: {}", foundChild);
        log.info("二쇰낫?몄옄: {}", foundChild.getPrimaryParent().get());
    }

    @Test
    @DisplayName("移섎즺?щ뒗 二쇰낫?몄옄 遺덇? - PARENT留?媛??)
    void testTherapistCannotBePrimary() {
        // Given - 移섎즺???ъ슜??
        User therapist = User.builder()
                .email("therapist@test.com")
                .password(passwordEncoder.encode("password"))
                .name("移섎즺??)
                .roles(Set.of(UserRole.THERAPIST))
                .build();
        userRepository.save(therapist);

        // Given - ?꾨룞
        Child child = Child.builder()
                .name("泥좎닔")
                .build();
        childRepository.save(child);

        // When & Then - 移섎즺?щ? 二쇰낫?몄옄濡??ㅼ젙 ?쒕룄
        ChildrenAuthorizedUser auth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(therapist)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isActive(true)
                .build();

        assertThatThrownBy(() -> child.addAuthorizedUser(auth))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("주보호자는 PARENT 역할만 가능합니다.");

        log.info("移섎즺??二쇰낫?몄옄 ?쒖빟 議곌굔 ?뺤긽 ?묐룞");
    }

    @Test
    @DisplayName("Soft Delete - ??젣???꾨룞? 議고쉶 ????)
    void testSoftDelete() {
        // Given
        Child child = Child.builder()
                .name("??젣?좎븘??)
                .build();
        childRepository.save(child);

        // When - Soft Delete
        child.delete();
        childRepository.save(child);
        childRepository.flush();

        // Then - ?쇰컲 議고쉶 ????蹂댁엫 (@SQLRestriction)
        Optional<Child> found = childRepository.findById(child.getChildId());

        assertThat(found).isEmpty();

        log.info("Soft Delete ?뺤긽 ?묐룞 - ??젣???꾨룞 議고쉶 ????);
    }

    @Test
    @DisplayName("二쇰낫?몄옄濡??꾨룞 紐⑸줉 議고쉶")
    @Transactional
    void testFindByPrimaryParent() {
        // Given - 遺紐?1紐? ?꾨룞 2紐?
        User parent = User.builder()
                .email("parent@test.com")
                .password(passwordEncoder.encode("password"))
                .name("遺紐?)
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(parent);

        Child child1 = Child.builder().name("泥レ㎏").build();
        Child child2 = Child.builder().name("?섏㎏").build();
        childRepository.saveAll(List.of(child1, child2));

        // 二쇰낫?몄옄 沅뚰븳 遺??
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
                .containsExactlyInAnyOrder("泥レ㎏", "?섏㎏");

        log.info("二쇰낫?몄옄???꾨룞 紐⑸줉: {}", children);
    }

    @Test
    @DisplayName("PIN ?ㅼ젙 諛?寃利?)
    void testPinCode() {
        // Given
        Child child = Child.builder()
                .name("PIN?뚯뒪??)
                .build();
        childRepository.save(child);

        String rawPin = "1234";
        String encryptedPin = passwordEncoder.encode(rawPin);

        // When - PIN ?ㅼ젙
        child.setPinCode(encryptedPin);
        childRepository.save(child);

        // Then
        Child found = childRepository.findById(child.getChildId()).orElseThrow();

        assertThat(found.getPinEnabled()).isTrue();
        assertThat(found.getPinCode()).isNotNull();
        assertThat(found.verifyPin("1234", passwordEncoder)).isTrue();
        assertThat(found.verifyPin("9999", passwordEncoder)).isFalse();

        log.info("PIN ?ㅼ젙 諛?寃利??깃났");
    }


    @Test
    @DisplayName("沅뚰븳蹂??꾪꽣留?議고쉶")
    @Transactional
    void testFindAccessibleChildren() {
        // Given - 遺紐? 移섎즺???앹꽦
        User parent = User.builder()
                .email("parent@test.com")
                .password(passwordEncoder.encode("password"))
                .name("遺紐?)
                .roles(Set.of(UserRole.PARENT))
                .build();

        User therapist = User.builder()
                .email("therapist@test.com")
                .password(passwordEncoder.encode("password"))
                .name("移섎즺??)
                .roles(Set.of(UserRole.THERAPIST))
                .build();

        userRepository.saveAll(List.of(parent, therapist));

        // Given - ?꾨룞 ?앹꽦
        Child child = Child.builder().name("沅뚰븳?뚯뒪??).build();
        childRepository.save(child);

        // 遺紐? 二쇰낫?몄옄
        ChildrenAuthorizedUser parentAuth = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .isActive(true)
                .build();
        child.addAuthorizedUser(parentAuth);

        // 移섎즺?? VIEW_REPORT留?
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

        log.info("遺紐??묎렐 媛???꾨룞: {}", parentChildren);
        log.info("移섎즺???묎렐 媛???꾨룞: {}", therapistChildren);
    }


    // ============= 2.23 異붽? ?뚯뒪??=============

    @Test
    @DisplayName("?꾨룞 ?앹꽦 - 吏꾨떒 ?뺣낫 諛??뱀씠?ы빆 ?ы븿")
    void testInsertChildWithDiagnosisInfo() {
        // Given
        Child child = Child.builder()
                .name("吏꾨떦?뺣낫?꾨룞")
                .birthDate(LocalDate.of(2018,7,20))
                .diagnosisDate(LocalDate.of(2021,3,15))
                .diagnosisInfo("?먰룓 ?ㅽ럺?몃읆 ?μ븷 寃쎌쬆")
                .specialNotes("???뚮━??誘쇨컧, 猷⑦떞 蹂寃???遺덉븞 利앷?")
                .build();

        // When
        Child saved = childRepository.save(child);

        // Then
        Child found = childRepository.findById(saved.getChildId()).orElseThrow();
        assertThat(found.getDiagnosisInfo()).isEqualTo("?먰룓 ?ㅽ럺?몃읆 ?μ븷 寃쎌쬆");
        assertThat(found.getSpecialNotes()).isEqualTo("???뚮━??誘쇨컧, 猷⑦떞 蹂寃???遺덉븞 利앷?");
        assertThat(found.getDiagnosisDate()).isEqualTo(LocalDate.of(2021, 3, 15));

        log.info("吏꾨떒 ?뺣낫 ?ы븿 ?꾨룞 ???완료: {}", found);
    }


    @Test
    @DisplayName("吏꾨떒 ?뺣낫 蹂寃?- 3000??珥덇낵 ???덉쇅")
    void testChangeDiagnosisInfoTooLong() {
        // Given
        Child child = Child.builder().name("?뚯뒪?몄븘??).build();
        childRepository.save(child);

        String tooLong = "A".repeat(3001);

        // When & Then
        assertThatThrownBy(() -> child.changeDiagnosisInfo(tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3000???댄븯");
    }

    @Test
    @DisplayName("?뱀씠?ы빆 蹂寃?- 3000??珥덇낵 ???덉쇅")
    void testChangeSpecialNotesTooLong() {
        // Given
        Child child = Child.builder().name("?뚯뒪?몄븘??).build();
        childRepository.save(child);

        String tooLong = "B".repeat(3001);

        // When & Then
        assertThatThrownBy(() -> child.changeSpecialNotes(tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3000???댄븯");
    }


    @Test
    @DisplayName("?좏샇 ?쒖젙 ?쒓렇 ???諛?議고쉶")
    @Transactional
    void testPreferredExpressions() {
        // Given
        Child child = Child.builder()
                .name("?쒖젙?꾨룞")
                .build();
        child.updatePreferredExpressions(Set.of(ExpressionTag.JOY, ExpressionTag.SURPRISE));
        childRepository.save(child);
        childRepository.flush();

        // When
        Child found =  childRepository.findById(child.getChildId()).orElseThrow();

        // Then
        assertThat(found.getPreferredExpressions())
                .containsExactlyInAnyOrder(ExpressionTag.JOY, ExpressionTag.SURPRISE);

        log.info("?좏샇 ?쒖젙 ?쒓렇: {}", found.getPreferredExpressions());
    }

    @Test
    @DisplayName("?대젮?뚰븯???쒖젙 ?쒓렇 ???諛?議고쉶")
    @Transactional
    void testDifficultExpressions() {
        // Given
        Child child = Child.builder()
                .name("?쒖젙?꾨룞")
                .build();
        child.updateDifficultExpressions(Set.of(ExpressionTag.ANGRY, ExpressionTag.FEAR));
        childRepository.save(child);
        childRepository.flush();

        // When
        Child found = childRepository.findById(child.getChildId()).orElseThrow();

        // Then
        assertThat(found.getDifficultExpressions())
                .containsExactlyInAnyOrder(ExpressionTag.ANGRY, ExpressionTag.FEAR);

        log.info("?대젮?뚰븯???쒖젙 ?쒓렇: {}", found.getDifficultExpressions());
    }

    @Test
    @DisplayName("?좏샇 ?쒖젙 ?쒓렇 媛깆떊 - 湲곗〈 ?쒓렇 援먯껜")
    @Transactional
    void testUpdatePreferredExpressionsReplaces() {
        // Given
        Child child = Child.builder().name("?쒓렇媛깆떊?꾨룞").build();
        child.updatePreferredExpressions(Set.of(ExpressionTag.JOY, ExpressionTag.SAD));
        childRepository.save(child);
        childRepository.flush();

        // When - ???쒓렇濡?援먯껜
        child.updatePreferredExpressions(Set.of(ExpressionTag.NEUTRAL));
        childRepository.flush();

        // Then
        Child found = childRepository.findById(child.getChildId()).orElseThrow();
        assertThat(found.getPreferredExpressions())
                .containsExactly(ExpressionTag.NEUTRAL)
                .doesNotContain(ExpressionTag.JOY, ExpressionTag.SAD);

        log.info("媛깆떊???좏샇 ?쒖젙 ?쒓렇: {}", found.getPreferredExpressions());
    }

    @Test
    @DisplayName("?좏샇 ?쒖젙 ?쒓렇 媛깆떊 - null ?꾨떖 ??鍮???)
    @Transactional
    void testUpdatePreferredExpressionsWithNull() {
        // Given
        Child child = Child.builder().name("?쒓렇珥덇린?붿븘??).build();
        child.updatePreferredExpressions(Set.of(ExpressionTag.JOY));
        childRepository.save(child);
        childRepository.flush();

        // When
        child.updatePreferredExpressions(null);
        childRepository.flush();

        // Then
        Child found = childRepository.findById(child.getChildId()).orElseThrow();
        assertThat(found.getPreferredExpressions()).isEmpty();
    }

    @Test
    @DisplayName("findByPreferredExpression - ?뱀젙 ?좏샇 ?쒖젙 ?쒓렇濡??꾨룞 議고쉶")
    @Transactional
    void testFindByPreferredExpression() {
        // Given
        Child child1 = Child.builder().name("湲곗겏??).build();
        child1.updatePreferredExpressions(Set.of(ExpressionTag.JOY, ExpressionTag.SURPRISE));

        Child child2 = Child.builder().name("?ы뵒??).build();
        child2.updatePreferredExpressions(Set.of(ExpressionTag.SAD));

        Child child3 = Child.builder().name("臾댄몴?뺤씠").build();
        child3.updatePreferredExpressions(Set.of(ExpressionTag.NEUTRAL));

        childRepository.saveAll(List.of(child1, child2, child3));
        childRepository.flush();

        // When
        List<Child> joyChildren = childRepository.findByPreferredExpression(ExpressionTag.JOY);

        // Then
        assertThat(joyChildren).hasSize(1);
        assertThat(joyChildren.get(0).getName()).isEqualTo("湲곗겏??);

        log.info("JOY ?좏샇 ?꾨룞: {}", joyChildren);
    }

    @Test
    @DisplayName("findByDifficultExpression - ?뱀젙 ?대젮???쒖젙 ?쒓렇濡??꾨룞 議고쉶")
    @Transactional
    void testFindByDifficultExpression() {
        // Given
        Child child1 = Child.builder().name("?붾궓??).build();
        child1.updateDifficultExpressions(Set.of(ExpressionTag.ANGRY));

        Child child2 = Child.builder().name("臾댁꽠??).build();
        child2.updateDifficultExpressions(Set.of(ExpressionTag.FEAR, ExpressionTag.ANGRY));

        Child child3 = Child.builder().name("??뚯씠").build();
        child3.updateDifficultExpressions(Set.of(ExpressionTag.SURPRISE));

        childRepository.saveAll(List.of(child1, child2, child3));
        childRepository.flush();

        // When
        List<Child> angryChildren = childRepository.findByDifficultExpression(ExpressionTag.ANGRY);

        // Then
        assertThat(angryChildren).hasSize(2);
        assertThat(angryChildren).extracting(Child::getName)
                .containsExactlyInAnyOrder("?붾궓??, "臾댁꽠??);

        log.info("ANGRY ?대젮???꾨룞: {}", angryChildren);
    }

    @Test
    @DisplayName("findByPreferredExpression - ?대떦 ?쒓렇 ?꾨룞 ?놁쑝硫?鍮?由ъ뒪??)
    @Transactional
    void testFindByPreferredExpressionEmpty() {
        // Given
        Child child = Child.builder().name("湲곗겏??).build();
        child.updatePreferredExpressions(Set.of(ExpressionTag.JOY));
        childRepository.save(child);
        childRepository.flush();

        // When
        List<Child> result = childRepository.findByPreferredExpression(ExpressionTag.SAD);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("?꾨줈??이미지 URL ???諛?蹂寃?)
    void testChangeProfileImageUrl() {
        // Given
        Child child = Child.builder().name("이미지?꾨룞").build();
        childRepository.save(child);

        // When - URL ?ㅼ젙
        String imageUrl = "https://storage.example.com/profiles/child123.png";
        child.changeProfileImageUrl(imageUrl);
        childRepository.save(child);

        // Then
        Child found = childRepository.findById(child.getChildId()).orElseThrow();
        assertThat(found.getProfileImageUrl()).isEqualTo(imageUrl);

        // When - URL ?쒓굅
        child.changeProfileImageUrl(null);
        childRepository.save(child);

        Child foundAfterRemove = childRepository.findById(child.getChildId()).orElseThrow();
        assertThat(foundAfterRemove.getProfileImageUrl()).isNull();

        log.info("?꾨줈??이미지 URL 蹂寃?완료");
    }

    @Test
    @DisplayName("?꾨줈??이미지 URL - 500??珥덇낵 ???덉쇅")
    void testChangeProfileImageUrlTooLong() {
        // Given
        Child child = Child.builder().name("?뚯뒪?몄븘??).build();
        childRepository.save(child);

        String tooLong = "https://example.com/" + "a".repeat(490);

        // When & Then
        assertThatThrownBy(() -> child.changeProfileImageUrl(tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500???댄븯");
    }
}
