package myexpressionfriend_api.child.service;

import myexpressionfriend_api.auth.domain.user.User;
import myexpressionfriend_api.auth.domain.user.UserRole;
import myexpressionfriend_api.auth.repository.UserRepository;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.domain.ChildPermissionType;
import myexpressionfriend_api.child.domain.ChildrenAuthorizedUser;
import myexpressionfriend_api.child.dto.AuthorizedUserResponseDTO;
import myexpressionfriend_api.child.dto.ChildAuthorizationCreateDTO;
import myexpressionfriend_api.child.dto.ChildAuthorizationUpdateDTO;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.child.repository.ChildrenAuthorizedUserRepository;
import myexpressionfriend_api.common.exception.ConflictException;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import myexpressionfriend_api.common.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChildAuthorizationService 단위 테스트")
class ChildAuthorizationServiceTest {

    @InjectMocks ChildAuthorizationService authorizationService;

    @Mock ChildRepository childRepository;
    @Mock UserRepository userRepository;
    @Mock ChildrenAuthorizedUserRepository authorizedUserRepository;

    private final UUID childId    = UUID.randomUUID();
    private final UUID grantorId  = UUID.randomUUID();
    private final UUID targetId   = UUID.randomUUID();

    private Child mockChild;
    private User targetUser;

    @BeforeEach
    void setUp() {
        mockChild  = mock(Child.class);
        targetUser = User.builder()
                .userId(targetId)        // getUserId() null 방지
                .email("target@example.com").name("대상자")
                .roles(Set.of(UserRole.THERAPIST)).build();
    }

    // ============= grantAuthorization =============

    @Nested
    @DisplayName("권한 부여")
    class Grant {

        @Test
        @DisplayName("주보호자가 권한 부여 시 성공한다")
        void grant_success() {
            ChildAuthorizationCreateDTO dto = ChildAuthorizationCreateDTO.builder()
                    .userId(targetId)
                    .permissions(Set.of(ChildPermissionType.VIEW_REPORT))
                    .build();

            User grantorUser = User.builder()
                    .userId(grantorId)   // getUserId() null 방지
                    .email("grantor@example.com").name("주보호자")
                    .roles(Set.of(UserRole.PARENT)).build();

            given(childRepository.findByIdWithAuthorizedUsers(childId)).willReturn(Optional.of(mockChild));
            given(mockChild.isPrimaryParent(grantorId)).willReturn(true);
            given(userRepository.findById(targetId)).willReturn(Optional.of(targetUser));
            given(authorizedUserRepository.existsByChildAndUser(mockChild, targetUser)).willReturn(false);
            given(mockChild.getPrimaryParent()).willReturn(Optional.of(grantorUser));
            given(authorizedUserRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            AuthorizedUserResponseDTO result = authorizationService.grantAuthorization(childId, grantorId, dto);

            assertThat(result).isNotNull();
            verify(authorizedUserRepository).save(any(ChildrenAuthorizedUser.class));
            verify(mockChild).addAuthorizedUser(any(ChildrenAuthorizedUser.class));
        }

        @Test
        @DisplayName("주보호자가 아닌 사용자가 권한 부여 시 InvalidRequestException이 발생한다")
        void grant_notPrimaryParent_throwsInvalidRequest() {
            ChildAuthorizationCreateDTO dto = ChildAuthorizationCreateDTO.builder()
                    .userId(targetId).build();

            given(childRepository.findByIdWithAuthorizedUsers(childId)).willReturn(Optional.of(mockChild));
            given(mockChild.isPrimaryParent(grantorId)).willReturn(false);

            assertThatThrownBy(() -> authorizationService.grantAuthorization(childId, grantorId, dto))
                    .isInstanceOf(InvalidRequestException.class);
            verify(authorizedUserRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 권한이 있는 사용자에게 부여 시 ConflictException이 발생한다")
        void grant_alreadyExists_throwsConflict() {
            ChildAuthorizationCreateDTO dto = ChildAuthorizationCreateDTO.builder()
                    .userId(targetId).build();

            given(childRepository.findByIdWithAuthorizedUsers(childId)).willReturn(Optional.of(mockChild));
            given(mockChild.isPrimaryParent(grantorId)).willReturn(true);
            given(userRepository.findById(targetId)).willReturn(Optional.of(targetUser));
            given(authorizedUserRepository.existsByChildAndUser(mockChild, targetUser)).willReturn(true);

            assertThatThrownBy(() -> authorizationService.grantAuthorization(childId, grantorId, dto))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("아동을 찾을 수 없으면 EntityNotFoundException이 발생한다")
        void grant_childNotFound_throwsEntityNotFound() {
            ChildAuthorizationCreateDTO dto = ChildAuthorizationCreateDTO.builder()
                    .userId(targetId).build();

            given(childRepository.findByIdWithAuthorizedUsers(childId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authorizationService.grantAuthorization(childId, grantorId, dto))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ============= revokeAuthorization =============

    @Nested
    @DisplayName("권한 해제")
    class Revoke {

        @Test
        @DisplayName("주보호자가 일반 권한자를 해제하면 성공한다")
        void revoke_success() {
            ChildrenAuthorizedUser authorization = mock(ChildrenAuthorizedUser.class);

            given(childRepository.findByIdWithAuthorizedUsers(childId)).willReturn(Optional.of(mockChild));
            given(mockChild.isPrimaryParent(grantorId)).willReturn(true);
            given(authorizedUserRepository.findByChildIdAndUserId(childId, targetId))
                    .willReturn(Optional.of(authorization));
            given(authorization.getIsPrimary()).willReturn(false);

            authorizationService.revokeAuthorization(childId, grantorId, targetId);

            verify(authorization).deactivate();
        }

        @Test
        @DisplayName("주보호자 권한을 해제하려 하면 InvalidRequestException이 발생한다")
        void revoke_targetIsPrimary_throwsInvalidRequest() {
            ChildrenAuthorizedUser authorization = mock(ChildrenAuthorizedUser.class);

            given(childRepository.findByIdWithAuthorizedUsers(childId)).willReturn(Optional.of(mockChild));
            given(mockChild.isPrimaryParent(grantorId)).willReturn(true);
            given(authorizedUserRepository.findByChildIdAndUserId(childId, targetId))
                    .willReturn(Optional.of(authorization));
            given(authorization.getIsPrimary()).willReturn(true);

            assertThatThrownBy(() -> authorizationService.revokeAuthorization(childId, grantorId, targetId))
                    .isInstanceOf(InvalidRequestException.class);
            verify(authorization, never()).deactivate();
        }

        @Test
        @DisplayName("주보호자가 아닌 사용자가 해제 시 InvalidRequestException이 발생한다")
        void revoke_notPrimaryParent_throwsInvalidRequest() {
            given(childRepository.findByIdWithAuthorizedUsers(childId)).willReturn(Optional.of(mockChild));
            given(mockChild.isPrimaryParent(grantorId)).willReturn(false);

            assertThatThrownBy(() -> authorizationService.revokeAuthorization(childId, grantorId, targetId))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ============= updateAuthorization =============

    @Nested
    @DisplayName("권한 수정")
    class Update {

        @Test
        @DisplayName("주보호자가 일반 권한자의 권한을 수정하면 성공한다")
        void update_success() {
            ChildrenAuthorizedUser authorization = mock(ChildrenAuthorizedUser.class);
            ChildAuthorizationUpdateDTO dto = ChildAuthorizationUpdateDTO.builder()
                    .permissions(Set.of(ChildPermissionType.PLAY_GAME, ChildPermissionType.VIEW_REPORT))
                    .isActive(true)
                    .build();

            given(childRepository.findByIdWithAuthorizedUsers(childId)).willReturn(Optional.of(mockChild));
            given(mockChild.isPrimaryParent(grantorId)).willReturn(true);
            given(authorizedUserRepository.findByChildIdAndUserId(childId, targetId))
                    .willReturn(Optional.of(authorization));
            given(authorization.getIsPrimary()).willReturn(false);
            given(authorization.getUser()).willReturn(targetUser);
            given(authorization.getPermissions()).willReturn(Set.of(ChildPermissionType.PLAY_GAME, ChildPermissionType.VIEW_REPORT));

            authorizationService.updateAuthorization(childId, grantorId, targetId, dto);

            verify(authorization).clearPermissions();
            verify(authorization).activate();
        }

        @Test
        @DisplayName("주보호자 권한을 수정하려 하면 InvalidRequestException이 발생한다")
        void update_targetIsPrimary_throwsInvalidRequest() {
            ChildrenAuthorizedUser authorization = mock(ChildrenAuthorizedUser.class);
            ChildAuthorizationUpdateDTO dto = ChildAuthorizationUpdateDTO.builder().build();

            given(childRepository.findByIdWithAuthorizedUsers(childId)).willReturn(Optional.of(mockChild));
            given(mockChild.isPrimaryParent(grantorId)).willReturn(true);
            given(authorizedUserRepository.findByChildIdAndUserId(childId, targetId))
                    .willReturn(Optional.of(authorization));
            given(authorization.getIsPrimary()).willReturn(true);

            assertThatThrownBy(() -> authorizationService.updateAuthorization(childId, grantorId, targetId, dto))
                    .isInstanceOf(InvalidRequestException.class);
        }
    }

    // ============= hasPermission =============

    @Nested
    @DisplayName("권한 보유 여부 확인")
    class HasPermission {

        @Test
        @DisplayName("권한이 있으면 true를 반환한다")
        void hasPermission_returnsTrue() {
            given(authorizedUserRepository.existsByChildIdAndUserIdAndPermission(
                    childId, grantorId, ChildPermissionType.PLAY_GAME)).willReturn(true);

            assertThat(authorizationService.hasPermission(childId, grantorId, ChildPermissionType.PLAY_GAME))
                    .isTrue();
        }

        @Test
        @DisplayName("권한이 없으면 false를 반환한다")
        void hasPermission_returnsFalse() {
            given(authorizedUserRepository.existsByChildIdAndUserIdAndPermission(
                    childId, grantorId, ChildPermissionType.MANAGE)).willReturn(false);

            assertThat(authorizationService.hasPermission(childId, grantorId, ChildPermissionType.MANAGE))
                    .isFalse();
        }
    }

    // ============= getAuthorizedUsers =============

    @Nested
    @DisplayName("권한 목록 조회")
    class GetAuthorizedUsers {

        @Test
        @DisplayName("접근 권한이 없으면 InvalidRequestException이 발생한다")
        void getAuthorizedUsers_noAccess_throwsInvalidRequest() {
            given(childRepository.findByIdWithAuthorizedUsers(childId)).willReturn(Optional.of(mockChild));
            given(mockChild.canAccess(grantorId)).willReturn(false);

            assertThatThrownBy(() -> authorizationService.getAuthorizedUsers(childId, grantorId))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("접근 권한이 있으면 활성화된 권한 목록을 반환한다")
        void getAuthorizedUsers_success() {
            ChildrenAuthorizedUser au = mock(ChildrenAuthorizedUser.class);
            given(au.getUser()).willReturn(targetUser);
            given(au.getPermissions()).willReturn(Set.of(ChildPermissionType.VIEW_REPORT));
            given(au.getIsPrimary()).willReturn(false);

            given(childRepository.findByIdWithAuthorizedUsers(childId)).willReturn(Optional.of(mockChild));
            given(mockChild.canAccess(grantorId)).willReturn(true);
            given(authorizedUserRepository.findActiveByChildId(childId)).willReturn(List.of(au));

            List<AuthorizedUserResponseDTO> result = authorizationService.getAuthorizedUsers(childId, grantorId);

            assertThat(result).hasSize(1);
        }
    }
}
