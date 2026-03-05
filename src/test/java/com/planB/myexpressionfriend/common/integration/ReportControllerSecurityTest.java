package com.planB.myexpressionfriend.common.integration;

import com.planB.myexpressionfriend.common.domain.user.UserRole;
import com.planB.myexpressionfriend.common.dto.note.PageResponseDTO;
import com.planB.myexpressionfriend.common.dto.report.GeneratedReportDTO;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.service.GeneratedReportService;
import com.planB.myexpressionfriend.common.service.ReportGenerationService;
import com.planB.myexpressionfriend.common.service.ReportPreferenceService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportPreferenceService reportPreferenceService;

    @MockitoBean
    private GeneratedReportService generatedReportService;

    @MockitoBean
    private ReportGenerationService reportGenerationService;

    @BeforeEach
    void setUp() {
        PageResponseDTO<GeneratedReportDTO> emptyPage = PageResponseDTO.<GeneratedReportDTO>builder()
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
        when(generatedReportService.getUserReportDTOs(any(UUID.class), any())).thenReturn(emptyPage);
    }

    @Test
    @DisplayName("ADMIN은 리포트 목록 조회가 거부된다")
    void reportsMe_admin_forbidden() throws Exception {
        mockMvc.perform(get("/api/reports/me").with(auth(UserRole.ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PARENT는 리포트 목록 조회가 허용된다")
    void reportsMe_parent_ok() throws Exception {
        mockMvc.perform(get("/api/reports/me").with(auth(UserRole.PARENT)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("THERAPIST는 리포트 목록 조회가 허용된다")
    void reportsMe_therapist_ok() throws Exception {
        mockMvc.perform(get("/api/reports/me").with(auth(UserRole.THERAPIST)))
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
