package com.planB.myexpressionfriend.common.integration;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import com.planB.myexpressionfriend.common.dto.child.AuthorizedUserDTO;
import com.planB.myexpressionfriend.common.dto.child.ChildAuthorizationDTO;
import com.planB.myexpressionfriend.common.dto.child.ChildCreateDTO;
import com.planB.myexpressionfriend.common.dto.child.ChildDTO;
import com.planB.myexpressionfriend.common.dto.child.ChildDetailDTO;
import com.planB.myexpressionfriend.common.dto.child.ChildUpdateDTO;
import com.planB.myexpressionfriend.common.dto.child.PinUpdateDTO;
import com.planB.myexpressionfriend.common.dto.child.PinVerificationDTO;
import com.planB.myexpressionfriend.common.dto.child.TransferPrimaryParentDTO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 아동 관리 통합 테스트
 * Service, Repository, Domain 레이어를 함께 검증한다.
 */
@SpringBootTest
@Transactional
@Slf4j
@DisplayName("아동 관리 통합 테스트")
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
        // 부모 사용자
        parent = User.builder()
                .email("parent@test.com")
                .password(passwordEncoder.encode("password"))
                .name("부모")
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(parent);

        // 치료사 사용자
        therapist = User.builder()
                .email("therapist@test.com")
                .password(passwordEncoder.encode("password"))
                .name("치료사")
                .roles(Set.of(UserRole.THERAPIST))
                .build();
        userRepository.save(therapist);

        // 다른 부모 사용자
        otherParent = User.builder()
                .email("other@test.com")
                .password(passwordEncoder.encode("password"))
                .name("다른부모")
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(otherParent);
    }

    @Test
    @DisplayName("전체 시나리오: 아동 생성 후 권한 관리와 조회, 수정, 삭제까지 수행한다")
    public void testFullScenario() {
        log.info("========== 1. 아동 생성 ==========");

        ChildCreateDTO childCreateDTO = ChildCreateDTO.builder()
                .name("테스트아동")
                .birthDate(LocalDate.of(2020, 5, 15))
                .gender("MALE")
                .diagnosisDate(LocalDate.of(2023, 1, 10))
                .pin("1234")
                .build();

        ChildDTO createdChild = childService.createChild(parent.getUserId(), childCreateDTO);

        assertThat(createdChild).isNotNull();
        assertThat(createdChild.getChildId()).isNotNull();
        assertThat(createdChild.getName()).isEqualTo("테스트아동");
        assertThat(createdChild.getIsPrimaryParent()).isTrue();
        assertThat(createdChild.getCanPlay()).isTrue();
        assertThat(createdChild.getPinEnabled()).isTrue();

        log.info("생성된 아동: {}", createdChild.getChildId());

        log.info("========== 2. 치료사 권한 부여 ==========");

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

        log.info("치료사 권한 부여 완료");

        log.info("========== 3. 내 아동 목록 조회 ==========");

        List<ChildDTO> myChildren = childService.getMyChildren(parent.getUserId());
        assertThat(myChildren).hasSize(1);
        assertThat(myChildren.get(0).getName()).isEqualTo("테스트아동");

        log.info("부모의 아동 수: {}", myChildren.size());

        log.info("========== 4. 접근 가능한 아동 조회(치료사) ==========");

        List<ChildDTO> accessibleChildren = childService.getAccessibleChildren(therapist.getUserId());
        assertThat(accessibleChildren).hasSize(1);
        assertThat(accessibleChildren.get(0).getCanPlay()).isFalse();

        log.info("치료사가 접근 가능한 아동 수: {}", accessibleChildren.size());

        log.info("========== 5. 플레이 가능한 아동 조회 ==========");

        List<ChildDTO> playableByParent = childService.getPlayableChildren(parent.getUserId());
        List<ChildDTO> playableByTherapist = childService.getPlayableChildren(therapist.getUserId());

        assertThat(playableByParent).hasSize(1);
        assertThat(playableByTherapist).isEmpty();

        log.info("부모 플레이 가능 아동 {}, 치료사 플레이 가능 아동 {}",
                playableByParent.size(), playableByTherapist.size());

        log.info("========== 6. 아동 상세 조회 ==========");

        ChildDetailDTO detail = childService.getChildDetail(createdChild.getChildId(), parent.getUserId());
        assertThat(detail).isNotNull();
        assertThat(detail.getPrimaryParent()).isNotNull();
        assertThat(detail.getPrimaryParent().getEmail()).isEqualTo("parent@test.com");
        assertThat(detail.getAuthorizedUsers()).hasSize(2);

        log.info("권한 사용자 수: {}", detail.getAuthorizedUsers().size());

        log.info("========== 7. 아동 정보 수정 ==========");

        ChildUpdateDTO updateDTO = ChildUpdateDTO.builder()
                .name("수정된아동")
                .gender("FEMALE")
                .build();

        ChildDTO updated = childService.updateChild(createdChild.getChildId(), parent.getUserId(), updateDTO);
        assertThat(updated.getName()).isEqualTo("수정된아동");
        assertThat(updated.getGender()).isEqualTo("FEMALE");

        log.info("아동 정보 수정 완료: {}", updated.getName());

        log.info("========== 8. PIN 검증 ==========");

        PinVerificationDTO pinDTO = PinVerificationDTO.builder()
                .pin("1234")
                .build();

        boolean isValid = childService.verifyPin(createdChild.getChildId(), parent.getUserId(), pinDTO);
        assertThat(isValid).isTrue();

        log.info("PIN 검증 성공");

        log.info("========== 9. 권한 목록 조회 ==========");

        List<AuthorizedUserDTO> authorizations = authorizationService.getAuthorizedUsers(
                createdChild.getChildId(),
                parent.getUserId()
        );

        assertThat(authorizations).hasSize(2);
        assertThat(authorizations)
                .extracting(AuthorizedUserDTO::getIsPrimary)
                .containsExactlyInAnyOrder(true, false);

        log.info("권한 목록 조회 완료: {}", authorizations.size());

        log.info("========== 10. 권한 수정 ==========");

        ChildAuthorizationDTO updateAuthDTO = ChildAuthorizationDTO.builder()
                .userId(therapist.getUserId())
                .permissions(Set.of(
                        ChildPermissionType.VIEW_REPORT,
                        ChildPermissionType.WRITE_NOTE,
                        ChildPermissionType.PLAY_GAME
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

        log.info("치료사 권한 수정 완료: PLAY_GAME 추가");

        log.info("========== 11. 권한 취소 ==========");

        authorizationService.revokeAuthorization(
                createdChild.getChildId(),
                parent.getUserId(),
                therapist.getUserId()
        );

        List<AuthorizedUserDTO> afterRevoke = authorizationService.getAuthorizedUsers(
                createdChild.getChildId(),
                parent.getUserId()
        );

        assertThat(afterRevoke).hasSize(1);
        log.info("치료사 권한 취소 완료");

        log.info("========== 12. 아동 삭제(Soft Delete) ==========");

        childService.deleteChild(createdChild.getChildId(), parent.getUserId());

        List<ChildDTO> afterDelete = childService.getMyChildren(parent.getUserId());
        assertThat(afterDelete).isEmpty();

        log.info("아동 삭제 완료");
    }

    @Test
    @DisplayName("보안 테스트: 다른 부모는 아동 상세에 접근할 수 없다")
    void testSecurityAccessDenied() {
        ChildCreateDTO createDTO = ChildCreateDTO.builder()
                .name("보안테스트아동")
                .birthDate(LocalDate.of(2020, 1, 1))
                .build();

        ChildDTO child = childService.createChild(parent.getUserId(), createDTO);

        assertThatThrownBy(() -> childService.getChildDetail(child.getChildId(), otherParent.getUserId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("권한");

        log.info("보안 테스트 통과: 권한 없는 사용자는 차단된다");
    }

    @Test
    @DisplayName("보안 테스트: 치료사는 주 보호자가 될 수 없다")
    void testTherapistCannotBePrimary() {
        ChildCreateDTO createDTO = ChildCreateDTO.builder()
                .name("치료사주보호자테스트")
                .birthDate(LocalDate.of(2020, 1, 1))
                .build();

        ChildDTO child = childService.createChild(parent.getUserId(), createDTO);

        ChildAuthorizationDTO authDTO = ChildAuthorizationDTO.builder()
                .userId(therapist.getUserId())
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .isPrimary(true)
                .build();

        assertThatThrownBy(() -> authorizationService.grantAuthorization(
                child.getChildId(),
                parent.getUserId(),
                authDTO
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PARENT");

        log.info("보안 테스트 통과: 치료사는 주 보호자가 될 수 없다");
    }

    @Test
    @DisplayName("보안 테스트: 주 보호자만 아동을 삭제할 수 있다")
    void testOnlyPrimaryCanDelete() {
        ChildCreateDTO createDTO = ChildCreateDTO.builder()
                .name("삭제권한테스트아동")
                .birthDate(LocalDate.of(2020, 1, 1))
                .build();

        ChildDTO child = childService.createChild(parent.getUserId(), createDTO);

        ChildAuthorizationDTO authDTO = ChildAuthorizationDTO.builder()
                .userId(therapist.getUserId())
                .permissions(Set.of(ChildPermissionType.MANAGE))
                .build();
        authorizationService.grantAuthorization(child.getChildId(), parent.getUserId(), authDTO);

        assertThatThrownBy(() -> childService.deleteChild(child.getChildId(), therapist.getUserId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("주 보호자");

        log.info("보안 테스트 통과: 주 보호자만 삭제할 수 있다");
    }

    @Test
    @DisplayName("제약 조건 테스트: 주 보호자는 한 명만 둘 수 있다")
    void testOnlyOnePrimaryParent() {
        ChildCreateDTO createDTO = ChildCreateDTO.builder()
                .name("주보호자제약테스트")
                .birthDate(LocalDate.of(2020, 1, 1))
                .build();

        ChildDTO child = childService.createChild(parent.getUserId(), createDTO);

        ChildAuthorizationDTO authDTO = ChildAuthorizationDTO.builder()
                .userId(otherParent.getUserId())
                .permissions(Set.of(ChildPermissionType.values()))
                .isPrimary(true)
                .build();

        assertThatThrownBy(() -> authorizationService.grantAuthorization(
                child.getChildId(),
                parent.getUserId(),
                authDTO
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1명");

        log.info("제약 조건 테스트 통과: 주 보호자는 한 명만 가능하다");
    }

    @Test
    @DisplayName("제약 조건 테스트: 사용자당 최대 다섯 명의 아동만 생성할 수 있다")
    void testMaxChildrenPerUser() {
        for (int i = 0; i < 5; i++) {
            ChildCreateDTO createDTO = ChildCreateDTO.builder()
                    .name("아동" + (i + 1))
                    .birthDate(LocalDate.of(2020, 1, 1))
                    .build();

            childService.createChild(parent.getUserId(), createDTO);
        }

        ChildCreateDTO sixthChild = ChildCreateDTO.builder()
                .name("아동6")
                .birthDate(LocalDate.of(2020, 1, 1))
                .build();

        assertThatThrownBy(() -> childService.createChild(parent.getUserId(), sixthChild))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("5명");

        log.info("제약 조건 테스트 통과: 최대 다섯 명만 등록 가능하다");
    }

    @Test
    @DisplayName("PIN 기능 테스트: 설정, 검증, 변경, 삭제가 가능하다")
    void testPinManagement() {
        ChildCreateDTO createDTO = ChildCreateDTO.builder()
                .name("PIN테스트아동")
                .birthDate(LocalDate.of(2020, 1, 1))
                .build();

        ChildDTO child = childService.createChild(parent.getUserId(), createDTO);
        assertThat(child.getPinEnabled()).isFalse();

        PinUpdateDTO setPinDTO = PinUpdateDTO.builder()
                .newPin("1234")
                .build();
        childService.updatePin(child.getChildId(), parent.getUserId(), setPinDTO);

        PinVerificationDTO verifyDTO = PinVerificationDTO.builder()
                .pin("1234")
                .build();
        boolean isValid = childService.verifyPin(child.getChildId(), parent.getUserId(), verifyDTO);
        assertThat(isValid).isTrue();

        PinUpdateDTO changePinDTO = PinUpdateDTO.builder()
                .currentPin("1234")
                .newPin("5678")
                .build();
        childService.updatePin(child.getChildId(), parent.getUserId(), changePinDTO);

        PinVerificationDTO verifyNewDTO = PinVerificationDTO.builder()
                .pin("5678")
                .build();
        boolean isNewValid = childService.verifyPin(child.getChildId(), parent.getUserId(), verifyNewDTO);
        assertThat(isNewValid).isTrue();

        childService.removePin(child.getChildId(), parent.getUserId(), "5678");

        Child updatedChild = childRepository.findById(child.getChildId()).orElseThrow();
        assertThat(updatedChild.getPinEnabled()).isFalse();

        log.info("PIN 기능 테스트 통과");
    }

    @Test
    @DisplayName("주 보호자 변경 테스트: 양육권 이전 시 새 보호자로 변경된다")
    void testTransferPrimaryParent() {
        ChildCreateDTO createDTO = ChildCreateDTO.builder()
                .name("주보호자변경테스트")
                .birthDate(LocalDate.of(2020, 1, 1))
                .pin("1234")
                .build();

        ChildDTO child = childService.createChild(parent.getUserId(), createDTO);

        ChildAuthorizationDTO authDTO = ChildAuthorizationDTO.builder()
                .userId(otherParent.getUserId())
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .build();
        authorizationService.grantAuthorization(child.getChildId(), parent.getUserId(), authDTO);

        TransferPrimaryParentDTO transferDTO = TransferPrimaryParentDTO.builder()
                .newPrimaryParentUserId(otherParent.getUserId())
                .pin("1234")
                .build();
        childService.transferPrimaryParent(child.getChildId(), parent.getUserId(), transferDTO);

        Child updatedChild = childRepository.findByIdWithAuthorizedUsers(child.getChildId()).orElseThrow();
        assertThat(updatedChild.isPrimaryParent(otherParent.getUserId())).isTrue();
        assertThat(updatedChild.isPrimaryParent(parent.getUserId())).isFalse();

        log.info("주 보호자 변경 테스트 통과");
    }

    @Test
    @DisplayName("권한 필터링 테스트: PLAY_GAME 권한에 따라 결과가 달라진다")
    void testPermissionFiltering() {
        ChildCreateDTO createDTO = ChildCreateDTO.builder()
                .name("권한필터테스트")
                .birthDate(LocalDate.of(2020, 1, 1))
                .build();

        ChildDTO child = childService.createChild(parent.getUserId(), createDTO);

        ChildAuthorizationDTO authDTO = ChildAuthorizationDTO.builder()
                .userId(therapist.getUserId())
                .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                .build();
        authorizationService.grantAuthorization(child.getChildId(), parent.getUserId(), authDTO);

        List<ChildDTO> parentPlayable = childService.getPlayableChildren(parent.getUserId());
        List<ChildDTO> therapistPlayable = childService.getPlayableChildren(therapist.getUserId());

        assertThat(parentPlayable).hasSize(1);
        assertThat(therapistPlayable).isEmpty();

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

        List<ChildDTO> therapistPlayableAfter = childService.getPlayableChildren(therapist.getUserId());
        assertThat(therapistPlayableAfter).hasSize(1);

        log.info("권한 필터링 테스트 통과");
    }
}