package com.planB.myexpressionfriend.common.integration;

import com.planB.myexpressionfriend.common.domain.user.UserRole;
import com.planB.myexpressionfriend.common.dto.note.PageResponseDTO;
import com.planB.myexpressionfriend.common.dto.notification.NotificationDTO;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        PageResponseDTO<NotificationDTO> emptyPage = PageResponseDTO.<NotificationDTO>builder()
                .content(List.of())
                .page(0)
                .size(20)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .hasNext(false)
                .hasPrevious(false)
                .build();
        when(notificationService.getNotifications(any(UUID.class), any())).thenReturn(emptyPage);
        doNothing().when(notificationService).markAsRead(any(UUID.class), any(UUID.class));
        when(notificationService.markAllAsRead(any(UUID.class))).thenReturn(0);
    }

    @Test
    @DisplayName("ADMIN? 알림 목록 조회媛 嫄곕??쒕떎")
    void notifications_admin_forbidden() throws Exception {
        mockMvc.perform(get("/api/notifications").with(auth(UserRole.ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PARENT??알림 목록 조회媛 ?덉슜?쒕떎")
    void notifications_parent_ok() throws Exception {
        mockMvc.perform(get("/api/notifications").with(auth(UserRole.PARENT)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("THERAPIST??알림 읽음 처리媛 ?덉슜?쒕떎")
    void notificationRead_therapist_ok() throws Exception {
        mockMvc.perform(patch("/api/notifications/{notificationId}/read", UUID.randomUUID())
                        .with(auth(UserRole.THERAPIST)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN? 알림 전체 읽음 처리媛 嫄곕??쒕떎")
    void notificationsReadAll_admin_forbidden() throws Exception {
        mockMvc.perform(patch("/api/notifications/read-all").with(auth(UserRole.ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PARENT??알림 전체 읽음 처리媛 ?덉슜?쒕떎")
    void notificationsReadAll_parent_ok() throws Exception {
        mockMvc.perform(patch("/api/notifications/read-all").with(auth(UserRole.PARENT)))
                .andExpect(status().isOk());
    }

    private RequestPostProcessor auth(UserRole role) {
        UserDTO principal = UserDTO.builder()
                .userId(UUID.randomUUID())
                .email(role.name().toLowerCase() + "@test.com")
                .password("pw")
                .name(role.name())
                .roles(Set.of(role))
                .build();
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(token);
    }
}
