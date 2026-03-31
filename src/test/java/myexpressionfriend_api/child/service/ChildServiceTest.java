package myexpressionfriend_api.child.service;

import myexpressionfriend_api.auth.domain.user.User;
import myexpressionfriend_api.auth.domain.user.UserRole;
import myexpressionfriend_api.auth.repository.UserRepository;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.domain.ChildPermissionType;
import myexpressionfriend_api.child.dto.ChildCreateDTO;
import myexpressionfriend_api.child.dto.ChildResponseDTO;
import myexpressionfriend_api.child.dto.ChildUpdateDTO;
import myexpressionfriend_api.child.repository.ChildRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChildService 단위 테스트")
class ChildServiceTest {

    @InjectMocks ChildService childService;

    @Mock ChildRepository childRepository;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    private final UUID creatorId = UUID.randomUUID();
    private final UUID childId   = UUID.randomUUID();

    private User parentUser;

    @BeforeEach
    void setUp() {
        parentUser = User.builder()
                .userId(creatorId)       // getUserId() null 방지
                .email("parent@example.com").name("부모")
                .roles(Set.of(UserRole.PARENT)).build();
    }

    // ============= createChild =============

    @Nested
    @DisplayName("아동 생성")
    class CreateChild {

        @Test
        @DisplayName("PARENT 역할 사용자가 아동을 생성하면 성공한다")
        void createChild_success() {
            ChildCreateDTO dto = ChildCreateDTO.builder().name("테스트아동").build();

            given(userRepository.findById(creatorId)).willReturn(Optional.of(parentUser));
            given(childRepository.save(any(Child.class))).willAnswer(inv -> inv.getArgument(0));

            ChildResponseDTO result = childService.createChild(dto, creatorId);

            assertThat(result).isNotNull();
            verify(childRepository).save(any(Child.class));
        }

        @Test
        @DisplayName("PARENT 역할이 없으면 InvalidRequestException이 발생한다")
        void createChild_notParent_throwsInvalidRequest() {
            User therapist = User.builder()
                    .email("t@example.com").name("치료사")
                    .roles(Set.of(UserRole.THERAPIST)).build();
            ChildCreateDTO dto = ChildCreateDTO.builder().name("테스트아동").build();

            given(userRepository.findById(creatorId)).willReturn(Optional.of(therapist));

            assertThatThrownBy(() -> childService.createChild(dto, creatorId))
                    .isInstanceOf(InvalidRequestException.class);
            verify(childRepository, never()).save(any());
        }

        @Test
        @DisplayName("사용자를 찾을 수 없으면 EntityNotFoundException이 발생한다")
        void createChild_userNotFound_throwsEntityNotFound() {
            ChildCreateDTO dto = ChildCreateDTO.builder().name("테스트아동").build();

            given(userRepository.findById(creatorId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> childService.createChild(dto, creatorId))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("PIN이 있으면 인코딩된 PIN이 설정된다")
        void createChild_withPin_encodesCalled() {
            ChildCreateDTO dto = ChildCreateDTO.builder().name("아동").pin("1234").build();

            given(userRepository.findById(creatorId)).willReturn(Optional.of(parentUser));
            given(passwordEncoder.encode("1234")).willReturn("encoded_pin");
            given(childRepository.save(any(Child.class))).willAnswer(inv -> inv.getArgument(0));

            childService.createChild(dto, creatorId);

            verify(passwordEncoder).encode("1234");
        }
    }

    // ============= updateChild =============

    @Nested
    @DisplayName("아동 수정")
    class UpdateChild {

        @Test
        @DisplayName("MANAGE 권한이 있으면 아동 정보를 수정한다")
        void updateChild_success() {
            Child mockChild = mock(Child.class);
            ChildUpdateDTO dto = ChildUpdateDTO.builder().name("수정된이름").build();

            given(childRepository.findByIdWithAuthorizedUsers(childId)).willReturn(Optional.of(mockChild));
            given(mockChild.hasPermission(creatorId, ChildPermissionType.MANAGE)).willReturn(true);
            given(mockChild.getAuthorizedUsers()).willReturn(new HashSet<>());

            childService.updateChild(childId, dto, creatorId);

            verify(mockChild).changeName("수정된이름");
        }

        @Test
        @DisplayName("MANAGE 권한이 없으면 InvalidRequestException이 발생한다")
        void updateChild_noPermission_throwsInvalidRequest() {
            Child mockChild = mock(Child.class);
            ChildUpdateDTO dto = ChildUpdateDTO.builder().name("수정").build();

            given(childRepository.findByIdWithAuthorizedUsers(childId)).willReturn(Optional.of(mockChild));
            given(mockChild.hasPermission(creatorId, ChildPermissionType.MANAGE)).willReturn(false);

            assertThatThrownBy(() -> childService.updateChild(childId, dto, creatorId))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("아동을 찾을 수 없으면 EntityNotFoundException이 발생한다")
        void updateChild_childNotFound_throwsEntityNotFound() {
            given(childRepository.findByIdWithAuthorizedUsers(childId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> childService.updateChild(childId, new ChildUpdateDTO(), creatorId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ============= deleteChild =============

    @Nested
    @DisplayName("아동 삭제")
    class DeleteChild {

        @Test
        @DisplayName("주보호자가 삭제하면 Soft Delete가 호출된다")
        void deleteChild_success() {
            Child mockChild = mock(Child.class);

            given(childRepository.findByIdWithAuthorizedUsers(childId)).willReturn(Optional.of(mockChild));
            given(mockChild.isPrimaryParent(creatorId)).willReturn(true);

            childService.deleteChild(childId, creatorId);

            verify(mockChild).delete(creatorId);
        }

        @Test
        @DisplayName("주보호자가 아니면 InvalidRequestException이 발생한다")
        void deleteChild_notPrimaryParent_throwsInvalidRequest() {
            Child mockChild = mock(Child.class);

            given(childRepository.findByIdWithAuthorizedUsers(childId)).willReturn(Optional.of(mockChild));
            given(mockChild.isPrimaryParent(creatorId)).willReturn(false);

            assertThatThrownBy(() -> childService.deleteChild(childId, creatorId))
                    .isInstanceOf(InvalidRequestException.class);
            verify(mockChild, never()).delete(any());
        }

        @Test
        @DisplayName("아동을 찾을 수 없으면 EntityNotFoundException이 발생한다")
        void deleteChild_childNotFound_throwsEntityNotFound() {
            given(childRepository.findByIdWithAuthorizedUsers(childId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> childService.deleteChild(childId, creatorId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }
}
