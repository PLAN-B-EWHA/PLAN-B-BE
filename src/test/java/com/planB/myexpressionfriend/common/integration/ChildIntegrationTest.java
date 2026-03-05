package com.planB.myexpressionfriend.common.integration;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import com.planB.myexpressionfriend.common.dto.child.*;
import com.planB.myexpressionfriend.common.repository.ChildRepository;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import com.planB.myexpressionfriend.common.service.ChildAuthorizationService;
import com.planB.myexpressionfriend.common.service.ChildService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;


/**
 * ?꾨룞 愿由??듯빀 ?뚯뒪??
 * Service + Repository + Domain 怨꾩링 ?듯빀
 */
@SpringBootTest
@Transactional
@Slf4j
@DisplayName("?꾨룞 愿由??듯빀 ?뚯뒪??)
public class ChildIntegrationTest {

    @Autowired
    private ChildService childService;

    @Autowired
    private ChildAuthorizationService authorizationService;

    @Autowired
    private ChildRepository childRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User parent;
    private User therapist;
    private User otherParent;

    @BeforeEach
    void setUp() {
        // 遺紐??ъ슜??
        parent = User.builder()
                .email("parent@test.com")
                .password(passwordEncoder.encode("password"))
                .name("遺紐?)
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(parent);

        // 移섎즺???ъ슜??
        therapist = User.builder()
                .email("therapist@test.com")
                .password(passwordEncoder.encode("password"))
                .name("移섎즺??)
                .roles(Set.of(UserRole.THERAPIST))
                .build();
        userRepository.save(therapist);

        // ?ㅻⅨ 遺紐??ъ슜??
        otherParent = User.builder()
                .email("other@test.com")
                .password(passwordEncoder.encode("password"))
                .name("?ㅻⅨ遺紐?)
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(otherParent);
    }

    @Test
    @DisplayName("?꾩껜 ?쒕굹由ъ삤: ?꾨룞 ?앹꽦 -> 沅뚰븳 遺??-> 議고쉶 -> ?섏젙 -> ??젣")
    public void testFullScenario() {

        // ========== 1. ?꾨룞 ?앹꽦 ==========
        log.info("========== 1. ?꾨룞 ?앹꽦 ==========");

        ChildCreateDTO childCreateDTO = ChildCreateDTO.builder()
                .name("?뚯뒪?몄븘??)
                .birthDate(LocalDate.of(2020, 5, 15))
                .gender("MALE")
                .diagnosisDate(LocalDate.of(2023, 1, 10))
                .pin("1234")
                .build();

        ChildDTO createdChild = childService.createChild(parent.getUserId(), childCreateDTO);

        assertThat(createdChild).isNotNull();
        assertThat(createdChild.getChildId()).isNotNull();
        assertThat(createdChild.getName()).isEqualTo("?뚯뒪?몄븘??);
        assertThat(createdChild.getIsPrimaryParent()).isTrue();
        assertThat(createdChild.getCanPlay()).isTrue();
        assertThat(createdChild.getPinEnabled()).isTrue();

        log.info("?앹꽦???꾨룞: {}", createdChild.getChildId());

        // ========== 2. 移섎즺?ъ뿉寃?沅뚰븳 遺??==========
        log.info("========== 2. 移섎즺?ъ뿉寃?沅뚰븳 遺??==========");

        ChildAuthorizationDTO authDTO = ChildAuthorizationDTO.builder()
                .userId(therapist.getUserId())
                .permissions(Set.of(
                        ChildPermissionType.VIEW_REPORT,
                        ChildPermissionType.WRITE_NOTE
                ))
                .isPrimary(false)
                .build();

        AuthorizedUserDTO authorization = authorizationService.grantAuthorization(
                createdChild.getChildId(),
                parent.getUserId(),
                authDTO
        );

        assertThat(authorization).isNotNull();
        assertThat(authorization.getIsPrimary()).isFalse();
        assertThat(authorization.getPermissions()).hasSize(2);

        log.info("移섎즺??沅뚰븳 遺??완료");

        // ========== 3. ???꾨룞 紐⑸줉 議고쉶 (遺紐? ==========
        log.info("========== 3. ???꾨룞 紐⑸줉 議고쉶 (遺紐? ==========");

        List<ChildDTO> myChildren = childService.getMyChildren(parent.getUserId());

        assertThat(myChildren).hasSize(1);
        assertThat(myChildren.get(0).getName()).isEqualTo("?뚯뒪?몄븘??);

        log.info("遺紐⑥쓽 ?꾨룞 紐⑸줉: {}", myChildren.size());

        // ========== 4. ?묎렐 媛?ν븳 ?꾨룞 議고쉶 (移섎즺?? ==========
        log.info("========== 4. ?묎렐 媛?ν븳 ?꾨룞 議고쉶 (移섎즺?? ==========");

        List<ChildDTO> accessibleChildren = childService.getAccessibleChildren(
                therapist.getUserId()
        );

        assertThat(accessibleChildren).hasSize(1);
        assertThat(accessibleChildren.get(0).getCanPlay()).isFalse();  // PLAY_GAME 沅뚰븳 ?놁쓬

        log.info("移섎즺?ъ쓽 ?묎렐 媛???꾨룞: {}", accessibleChildren.size());

        // ========== 5. ?뚮젅??媛?ν븳 ?꾨룞 議고쉶 ==========
        log.info("========== 5. ?뚮젅??媛?ν븳 ?꾨룞 議고쉶 ==========");

        List<ChildDTO> playableByParent = childService.getPlayableChildren(parent.getUserId());
        List<ChildDTO> playableByTherapist = childService.getPlayableChildren(therapist.getUserId());

        assertThat(playableByParent).hasSize(1);  // 遺紐⑤뒗 ?뚮젅??媛??
        assertThat(playableByTherapist).isEmpty();  // 移섎즺?щ뒗 PLAY_GAME 沅뚰븳 ?놁쓬

        log.info("遺紐??뚮젅??媛?? {}, 移섎즺???뚮젅??媛?? {}",
                playableByParent.size(), playableByTherapist.size());

        // ========== 6. ?꾨룞 ?곸꽭 議고쉶 ==========
        log.info("========== 6. ?꾨룞 ?곸꽭 議고쉶 ==========");

        ChildDetailDTO detail = childService.getChildDetail(
                createdChild.getChildId(),
                parent.getUserId()
        );

        assertThat(detail).isNotNull();
        assertThat(detail.getPrimaryParent()).isNotNull();
        assertThat(detail.getPrimaryParent().getEmail()).isEqualTo("parent@test.com");
        assertThat(detail.getAuthorizedUsers()).hasSize(2);  // 遺紐?+ 移섎즺??

        log.info("沅뚰븳 紐⑸줉: {}", detail.getAuthorizedUsers().size());

        // ========== 7. ?꾨룞 ?뺣낫 ?섏젙 ==========
        log.info("========== 7. ?꾨룞 ?뺣낫 ?섏젙 ==========");

        ChildUpdateDTO updateDTO = ChildUpdateDTO.builder()
                .name("?섏젙?쒖씠由?)
                .gender("FEMALE")
                .build();

        ChildDTO updated = childService.updateChild(
                createdChild.getChildId(),
                parent.getUserId(),
                updateDTO
        );

        assertThat(updated.getName()).isEqualTo("?섏젙?쒖씠由?);
        assertThat(updated.getGender()).isEqualTo("FEMALE");

        log.info("?꾨룞 ?뺣낫 ?섏젙 완료: {}", updated.getName());

        // ========== 8. PIN 寃利?==========
        log.info("========== 8. PIN 寃利?==========");

        PinVerificationDTO pinDTO = PinVerificationDTO.builder()
                .pin("1234")
                .build();

        boolean isValid = childService.verifyPin(
                createdChild.getChildId(),
                parent.getUserId(),
                pinDTO
        );

        assertThat(isValid).isTrue();

        log.info("PIN 寃利??깃났");

        // ========== 9. 沅뚰븳 紐⑸줉 議고쉶 ==========
        log.info("========== 9. 沅뚰븳 紐⑸줉 議고쉶 ==========");

        List<AuthorizedUserDTO> authorizations = authorizationService.getAuthorizedUsers(
                createdChild.getChildId(),
                parent.getUserId()
        );

        assertThat(authorizations).hasSize(2);
        assertThat(authorizations)
                .extracting(AuthorizedUserDTO::getIsPrimary)
                .containsExactlyInAnyOrder(true, false);

        log.info("沅뚰븳 紐⑸줉 議고쉶 완료: {}", authorizations.size());

        // ========== 10. 沅뚰븳 ?섏젙 ==========
        log.info("========== 10. 沅뚰븳 ?섏젙 ==========");

        ChildAuthorizationDTO updateAuthDTO = ChildAuthorizationDTO.builder()
                .userId(therapist.getUserId())
                .permissions(Set.of(
                        ChildPermissionType.VIEW_REPORT,
                        ChildPermissionType.WRITE_NOTE,
                        ChildPermissionType.PLAY_GAME  // 異붽?
                ))
                .build();

        AuthorizedUserDTO updatedAuth = authorizationService.updateAuthorization(
                createdChild.getChildId(),
                parent.getUserId(),
                therapist.getUserId(),
                updateAuthDTO
        );

        assertThat(updatedAuth.getPermissions()).hasSize(3);
        assertThat(updatedAuth.getPermissions()).contains(ChildPermissionType.PLAY_GAME);

        log.info("移섎즺??沅뚰븳 ?섏젙 완료: PLAY_GAME 異붽?");

        // ========== 11. 沅뚰븳 痍⑥냼 ==========
        log.info("========== 11. 沅뚰븳 痍⑥냼 ==========");

        authorizationService.revokeAuthorization(
                createdChild.getChildId(),
                parent.getUserId(),
                therapist.getUserId()
        );

        List<AuthorizedUserDTO> afterRevoke = authorizationService.getAuthorizedUsers(
                createdChild.getChildId(),
                parent.getUserId()
        );

        assertThat(afterRevoke).hasSize(1);  // 遺紐⑤쭔 ?⑥쓬

        log.info("移섎즺??沅뚰븳 痍⑥냼 완료");

        // ========== 12. ?꾨룞 ??젣 (Soft Delete) ==========
        log.info("========== 12. ?꾨룞 ??젣 (Soft Delete) ==========");

        childService.deleteChild(createdChild.getChildId(), parent.getUserId());

        List<ChildDTO> afterDelete = childService.getMyChildren(parent.getUserId());

        assertThat(afterDelete).isEmpty();  // Soft Delete濡?議고쉶 ????

        log.info("?꾨룞 ??젣 완료 (Soft Delete)");
    }

    @Test
    @DisplayName("蹂댁븞 ?뚯뒪?? ?ㅻⅨ ?ъ슜?먮뒗 ?꾨룞 ?묎렐 遺덇?")
    void testSecurityAccessDenied() {

        // Given - 遺紐????꾨룞 ?앹꽦
        ChildCreateDTO createDTO = ChildCreateDTO.builder()
                .name("蹂댁븞?뚯뒪?몄븘??)
                .birthDate(LocalDate.of(2020, 1, 1))
                .build();

        ChildDTO child = childService.createChild(parent.getUserId(), createDTO);

        // When & Then - 遺紐????묎렐 遺덇?
        assertThatThrownBy(() ->
                childService.getChildDetail(child.getChildId(), otherParent.getUserId())
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("접근 권한이 없습니다.");

        log.info("蹂댁븞 ?뚯뒪???듦낵: 沅뚰븳 ?녿뒗 ?ъ슜??李⑤떒");
    }

    @Test
    @DisplayName("蹂댁븞 ?뚯뒪?? 移섎즺?щ뒗 二쇰낫?몄옄媛 ?????놁쓬")
    void testTherapistCannotBePrimary() {
        // Given - ?꾨룞 ?앹꽦
        ChildCreateDTO createDTO = ChildCreateDTO.builder()
                .name("移섎즺?ъ젣?쏀뀒?ㅽ듃")
                .birthDate(LocalDate.of(2020, 1, 1))
                .build();

        ChildDTO child = childService.createChild(parent.getUserId(), createDTO);

        // When & Then - 移섎즺?щ? 二쇰낫?몄옄濡??ㅼ젙 ?쒕룄
        ChildAuthorizationDTO authDTO = ChildAuthorizationDTO.builder()
                .userId(therapist.getUserId())
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isPrimary(true)  // 二쇰낫?몄옄 ?쒕룄
                .build();

        assertThatThrownBy(() ->
                authorizationService.grantAuthorization(
                        child.getChildId(),
                        parent.getUserId(),
                        authDTO
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("주보호자는 PARENT 역할만 가능합니다.");

        log.info("蹂댁븞 ?뚯뒪???듦낵: 移섎즺??二쇰낫?몄옄 李⑤떒");
    }

    @Test
    @DisplayName("蹂댁븞 ?뚯뒪?? 二쇰낫?몄옄留??꾨룞 ??젣 媛??)
    void testOnlyPrimaryCanDelete() {
        // Given - ?꾨룞 ?앹꽦 諛?移섎즺??沅뚰븳 遺??
        ChildCreateDTO createDTO = ChildCreateDTO.builder()
                .name("??젣沅뚰븳?뚯뒪??)
                .birthDate(LocalDate.of(2020, 1, 1))
                .build();

        ChildDTO child = childService.createChild(parent.getUserId(), createDTO);

        ChildAuthorizationDTO authDTO = ChildAuthorizationDTO.builder()
                .userId(therapist.getUserId())
                .permissions(Set.of(ChildPermissionType.MANAGE))  // MANAGE 沅뚰븳??遺??
                .build();

        authorizationService.grantAuthorization(
                child.getChildId(),
                parent.getUserId(),
                authDTO
        );

        // When & Then - 移섎즺?щ뒗 ??젣 遺덇?
        assertThatThrownBy(() ->
                childService.deleteChild(child.getChildId(), therapist.getUserId())
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("二쇰낫?몄옄留??꾨룞????젣?????덉뒿?덈떎");

        log.info("蹂댁븞 ?뚯뒪???듦낵: 鍮꾩＜蹂댄샇????젣 李⑤떒");
    }

    @Test
    @DisplayName("?쒖빟 議곌굔 ?뚯뒪?? 二쇰낫?몄옄??1紐낅쭔")
    void testOnlyOnePrimaryParent() {
        // Given - ?꾨룞 ?앹꽦
        ChildCreateDTO createDTO = ChildCreateDTO.builder()
                .name("二쇰낫?몄옄?쒖빟?뚯뒪??)
                .birthDate(LocalDate.of(2020, 1, 1))
                .build();

        ChildDTO child = childService.createChild(parent.getUserId(), createDTO);

        // When & Then - ?ㅻⅨ 遺紐⑤? 二쇰낫?몄옄濡?異붽? ?쒕룄
        ChildAuthorizationDTO authDTO = ChildAuthorizationDTO.builder()
                .userId(otherParent.getUserId())
                .permissions(Set.of(ChildPermissionType.values()))
                .isPrimary(true)
                .build();

        assertThatThrownBy(() ->
                authorizationService.grantAuthorization(
                        child.getChildId(),
                        parent.getUserId(),
                        authDTO
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("주보호자는 1명만 가능합니다.");

        log.info("?쒖빟 議곌굔 ?뚯뒪???듦낵: 二쇰낫?몄옄 1紐??쒗븳");
    }

    @Test
    @DisplayName("?쒖빟 議곌굔 ?뚯뒪?? ?ъ슜?먮떦 理쒕? 5紐??꾨룞")
    void testMaxChildrenPerUser() {
        // Given - 5紐낆쓽 ?꾨룞 ?앹꽦
        for (int i = 0; i < 5; i++) {
            ChildCreateDTO createDTO = ChildCreateDTO.builder()
                    .name("?꾨룞" + (i + 1))
                    .birthDate(LocalDate.of(2020, 1, 1))
                    .build();

            childService.createChild(parent.getUserId(), createDTO);
        }

        // When & Then - 6踰덉㎏ ?꾨룞 ?앹꽦 ?쒕룄
        ChildCreateDTO sixthChild = ChildCreateDTO.builder()
                .name("6踰덉㎏?꾨룞")
                .birthDate(LocalDate.of(2020, 1, 1))
                .build();

        assertThatThrownBy(() ->
                childService.createChild(parent.getUserId(), sixthChild)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("理쒕? 5紐낆쓽 ?꾨룞留??깅줉?????덉뒿?덈떎");

        log.info("?쒖빟 議곌굔 ?뚯뒪???듦낵: 理쒕? 5紐??쒗븳");
    }

    @Test
    @DisplayName("PIN 湲곕뒫 ?뚯뒪?? ?ㅼ젙/寃利?蹂寃??쒓굅")
    void testPinManagement() {
        // Given - PIN ?놁씠 ?꾨룞 ?앹꽦
        ChildCreateDTO createDTO = ChildCreateDTO.builder()
                .name("PIN?뚯뒪??)
                .birthDate(LocalDate.of(2020, 1, 1))
                .build();

        ChildDTO child = childService.createChild(parent.getUserId(), createDTO);
        assertThat(child.getPinEnabled()).isFalse();

        // When - PIN ?ㅼ젙
        PinUpdateDTO setPinDTO = PinUpdateDTO.builder()
                .newPin("1234")
                .build();

        childService.updatePin(child.getChildId(), parent.getUserId(), setPinDTO);

        // Then - PIN 寃利?
        PinVerificationDTO verifyDTO = PinVerificationDTO.builder()
                .pin("1234")
                .build();

        boolean isValid = childService.verifyPin(
                child.getChildId(),
                parent.getUserId(),
                verifyDTO
        );

        assertThat(isValid).isTrue();

        // When - PIN 蹂寃?
        PinUpdateDTO changePinDTO = PinUpdateDTO.builder()
                .currentPin("1234")
                .newPin("5678")
                .build();

        childService.updatePin(child.getChildId(), parent.getUserId(), changePinDTO);

        // Then - ??PIN 寃利?
        PinVerificationDTO verifyNewDTO = PinVerificationDTO.builder()
                .pin("5678")
                .build();

        boolean isNewValid = childService.verifyPin(
                child.getChildId(),
                parent.getUserId(),
                verifyNewDTO
        );

        assertThat(isNewValid).isTrue();

        // When - PIN ?쒓굅
        childService.removePin(child.getChildId(), parent.getUserId(), "5678");

        // Then - ?쒓굅 ?뺤씤
        Child updatedChild = childRepository.findById(child.getChildId()).orElseThrow();
        assertThat(updatedChild.getPinEnabled()).isFalse();

        log.info("PIN 愿由??뚯뒪???듦낵");
    }

    @Test
    @DisplayName("二쇰낫?몄옄 蹂寃??뚯뒪??(?묒쑁沅??댁쟾)")
    void testTransferPrimaryParent() {
        // Given - ?꾨룞 ?앹꽦
        ChildCreateDTO createDTO = ChildCreateDTO.builder()
                .name("二쇰낫?몄옄蹂寃쏀뀒?ㅽ듃")
                .birthDate(LocalDate.of(2020, 1, 1))
                .pin("1234")
                .build();

        ChildDTO child = childService.createChild(parent.getUserId(), createDTO);

        // ?ㅻⅨ 遺紐⑥뿉寃?癒쇱? 沅뚰븳 遺??
        ChildAuthorizationDTO authDTO = ChildAuthorizationDTO.builder()
                .userId(otherParent.getUserId())
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .build();

        authorizationService.grantAuthorization(
                child.getChildId(),
                parent.getUserId(),
                authDTO
        );

        // When - 二쇰낫?몄옄 蹂寃?
        TransferPrimaryParentDTO transferDTO = TransferPrimaryParentDTO.builder()
                .newPrimaryParentUserId(otherParent.getUserId())
                .pin("1234")
                .build();

        childService.transferPrimaryParent(
                child.getChildId(),
                parent.getUserId(),
                transferDTO
        );

        // Then - 蹂寃??뺤씤
        Child updatedChild = childRepository.findByIdWithAuthorizedUsers(
                child.getChildId()
        ).orElseThrow();

        assertThat(updatedChild.isPrimaryParent(otherParent.getUserId())).isTrue();
        assertThat(updatedChild.isPrimaryParent(parent.getUserId())).isFalse();

        log.info("二쇰낫?몄옄 蹂寃??뚯뒪???듦낵");
    }

    @Test
    @DisplayName("沅뚰븳蹂??꾪꽣留??뚯뒪??)
    void testPermissionFiltering() {
        // Given - ?꾨룞 ?앹꽦
        ChildCreateDTO createDTO = ChildCreateDTO.builder()
                .name("沅뚰븳?꾪꽣留곹뀒?ㅽ듃")
                .birthDate(LocalDate.of(2020, 1, 1))
                .build();

        ChildDTO child = childService.createChild(parent.getUserId(), createDTO);

        // 移섎즺?ъ뿉寃?VIEW_REPORT留?遺??
        ChildAuthorizationDTO authDTO = ChildAuthorizationDTO.builder()
                .userId(therapist.getUserId())
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .build();

        authorizationService.grantAuthorization(
                child.getChildId(),
                parent.getUserId(),
                authDTO
        );

        // When & Then
        List<ChildDTO> parentPlayable = childService.getPlayableChildren(parent.getUserId());
        List<ChildDTO> therapistPlayable = childService.getPlayableChildren(therapist.getUserId());

        assertThat(parentPlayable).hasSize(1);  // PARENT??紐⑤뱺 沅뚰븳
        assertThat(therapistPlayable).isEmpty();  // THERAPIST??PLAY_GAME ?놁쓬

        // 移섎즺?ъ뿉寃?PLAY_GAME 異붽?
        ChildAuthorizationDTO updateAuthDTO = ChildAuthorizationDTO.builder()
                .userId(therapist.getUserId())
                .permissions(Set.of(
                        ChildPermissionType.VIEW_REPORT,
                        ChildPermissionType.PLAY_GAME
                ))
                .build();

        authorizationService.updateAuthorization(
                child.getChildId(),
                parent.getUserId(),
                therapist.getUserId(),
                updateAuthDTO
        );

        List<ChildDTO> therapistPlayableAfter = childService.getPlayableChildren(
                therapist.getUserId()
        );

        assertThat(therapistPlayableAfter).hasSize(1);  // ?댁젣 ?뚮젅??媛??

        log.info("沅뚰븳蹂??꾪꽣留??뚯뒪???듦낵");
    }
}
