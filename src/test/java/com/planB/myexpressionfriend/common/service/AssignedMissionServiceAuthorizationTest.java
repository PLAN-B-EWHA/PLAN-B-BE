package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.mission.MissionStatus;
import com.planB.myexpressionfriend.common.dto.note.PageResponseDTO;
import com.planB.myexpressionfriend.common.repository.AssignedMissionRepository;
import com.planB.myexpressionfriend.common.repository.ChildNoteRepository;
import com.planB.myexpressionfriend.common.repository.ChildRepository;
import com.planB.myexpressionfriend.common.repository.MissionTemplateRepository;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignedMissionServiceAuthorizationTest {

    @Mock
    private AssignedMissionRepository missionRepository;
    @Mock
    private MissionTemplateRepository templateRepository;
    @Mock
    private ChildRepository childRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChildNoteRepository childNoteRepository;
    @Mock
    private ChildNoteService noteService;
    @Mock
    private ChildAuthorizationService childAuthorizationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AssignedMissionService assignedMissionService;

    @Test
    @DisplayName("VIEW_REPORT 권한이 없으면 아동 미션 목록 조회가 거부된다")
    void getMissionsByChild_withoutViewReport_throwsAccessDenied() {
        UUID childId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        when(childAuthorizationService.hasPermission(childId, userId, ChildPermissionType.VIEW_REPORT))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> assignedMissionService.getMissionsByChild(childId, userId, pageable));

        verify(missionRepository, never()).findByChildIdWithAuth(eq(childId), eq(userId), eq(pageable));
    }

    @Test
    @DisplayName("VIEW_REPORT 권한이 있으면 아동 미션 목록 조회가 허용된다")
    void getMissionsByChild_withViewReport_ok() {
        UUID childId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        when(childAuthorizationService.hasPermission(childId, userId, ChildPermissionType.VIEW_REPORT))
                .thenReturn(true);
        when(missionRepository.findByChildIdWithAuth(childId, userId, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponseDTO<?> result = assignedMissionService.getMissionsByChild(childId, userId, pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(missionRepository).findByChildIdWithAuth(childId, userId, pageable);
    }

    @Test
    @DisplayName("VIEW_REPORT 권한이 없으면 미션 개수 조회가 거부된다")
    void countMissions_withoutViewReport_throwsAccessDenied() {
        UUID childId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(childAuthorizationService.hasPermission(childId, userId, ChildPermissionType.VIEW_REPORT))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> assignedMissionService.countMissionsByChild(childId, userId));

        verify(missionRepository, never()).countByChildIdWithAuth(childId, userId);
    }

    @Test
    @DisplayName("VIEW_REPORT 권한이 있으면 상태별 미션 개수 조회가 허용된다")
    void countMissionsByStatus_withViewReport_ok() {
        UUID childId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(childAuthorizationService.hasPermission(childId, userId, ChildPermissionType.VIEW_REPORT))
                .thenReturn(true);
        when(missionRepository.countByChildIdAndStatusWithAuth(childId, userId, MissionStatus.COMPLETED))
                .thenReturn(3L);

        long count = assignedMissionService.countMissionsByChildAndStatus(childId, userId, MissionStatus.COMPLETED);

        assertEquals(3L, count);
        verify(missionRepository).countByChildIdAndStatusWithAuth(childId, userId, MissionStatus.COMPLETED);
    }
}

