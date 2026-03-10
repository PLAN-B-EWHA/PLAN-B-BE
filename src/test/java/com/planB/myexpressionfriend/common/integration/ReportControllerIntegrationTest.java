package com.planB.myexpressionfriend.common.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planB.myexpressionfriend.common.domain.report.GeneratedReport;
import com.planB.myexpressionfriend.common.domain.report.ReportChildScope;
import com.planB.myexpressionfriend.common.domain.report.ReportDeliveryChannel;
import com.planB.myexpressionfriend.common.domain.report.ReportPreference;
import com.planB.myexpressionfriend.common.domain.report.ReportScheduleType;
import com.planB.myexpressionfriend.common.domain.report.ReportStatus;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import com.planB.myexpressionfriend.common.dto.report.ReportPreferenceUpdateDTO;
import com.planB.myexpressionfriend.common.repository.GeneratedReportRepository;
import com.planB.myexpressionfriend.common.repository.ReportPreferenceRepository;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Report Controller 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("Report Controller 통합 테스트")
public class ReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportPreferenceRepository preferenceRepository;

    @Autowired
    private GeneratedReportRepository reportRepository;

    private User parent;
    private User otherUser;

    private ReportPreference preference;
    private GeneratedReport generatedReport;
    private GeneratedReport pendingReport;

    @BeforeEach
    void setUp() {
        parent = User.builder()
                .email("parent@test.com")
                .password("encoded-password")
                .name("부모")
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(parent);

        otherUser = User.builder()
                .email("other@test.com")
                .password("encoded-password")
                .name("다른부모")
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(otherUser);

        preference = ReportPreference.builder()
                .userId(parent.getUserId())
                .enabled(true)
                .scheduleType(ReportScheduleType.WEEKLY)
                .deliveryChannel(ReportDeliveryChannel.IN_APP)
                .deliveryTime(LocalTime.of(9, 0))
                .timezone("Asia/Seoul")
                .childScope(ReportChildScope.ALL_CHILDREN)
                .language("ko")
                .modelName("gpt-4o")
                .maxTokens(1200)
                .cooldownHours(24)
                .autoIssueOnNoData(false)
                .build();
        preferenceRepository.save(preference);

        generatedReport = GeneratedReport.builder()
                .userId(parent.getUserId())
                .preferenceId(preference.getPreferenceId())
                .status(ReportStatus.GENERATED)
                .periodStartAt(LocalDateTime.now().minusDays(7))
                .periodEndAt(LocalDateTime.now())
                .build();
        generatedReport.markGenerated(
                "주간 발달 리포트",
                "아동의 참여와 반응이 전반적으로 안정적이었습니다.",
                "관찰 기간 동안 과제 참여도와 상호작용 반응이 꾸준히 유지되었고, 지난 10일 대비 집중 시간이 증가했습니다.",
                "요약 정보",
                "gpt-4o",
                LocalDateTime.now()
        );
        reportRepository.save(generatedReport);

        pendingReport = GeneratedReport.builder()
                .userId(parent.getUserId())
                .preferenceId(preference.getPreferenceId())
                .status(ReportStatus.PENDING)
                .periodStartAt(LocalDateTime.now().minusDays(14))
                .periodEndAt(LocalDateTime.now().minusDays(7))
                .build();
        reportRepository.save(pendingReport);
    }

    @AfterEach
    void tearDown() {
        TestSecurityConfig.clearAuthentication();
    }

    @Test
    @DisplayName("내 리포트 설정 조회 성공")
    void getMyPreference_Exists_Success() throws Exception {
        TestSecurityConfig.setAuthentication(parent);

        mockMvc.perform(get("/api/reports/preferences/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(parent.getUserId().toString()))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.modelName").value("gpt-4o"));
    }

    @Test
    @DisplayName("리포트 설정이 없으면 기본값으로 생성된다")
    void getMyPreference_NotExists_CreatesDefault() throws Exception {
        TestSecurityConfig.setAuthentication(otherUser);

        mockMvc.perform(get("/api/reports/preferences/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(otherUser.getUserId().toString()))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.maxTokens").value(1200));
    }

    @Test
    @DisplayName("리포트 설정 사용 여부 수정 성공")
    void updateMyPreference_Enabled_Success() throws Exception {
        TestSecurityConfig.setAuthentication(parent);
        ReportPreferenceUpdateDTO updateDTO = ReportPreferenceUpdateDTO.builder()
                .enabled(false)
                .build();

        mockMvc.perform(put("/api/reports/preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    @DisplayName("리포트 설정 스케줄 타입 수정 성공")
    void updateMyPreference_ScheduleType_Success() throws Exception {
        TestSecurityConfig.setAuthentication(parent);
        ReportPreferenceUpdateDTO updateDTO = ReportPreferenceUpdateDTO.builder()
                .scheduleType(ReportScheduleType.DAILY)
                .build();

        mockMvc.perform(put("/api/reports/preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scheduleType").value("DAILY"));
    }

    @Test
    @DisplayName("리포트 설정 전달 채널 수정 성공")
    void updateMyPreference_DeliveryChannel_Success() throws Exception {
        TestSecurityConfig.setAuthentication(parent);
        ReportPreferenceUpdateDTO updateDTO = ReportPreferenceUpdateDTO.builder()
                .deliveryChannel(ReportDeliveryChannel.EMAIL)
                .build();

        mockMvc.perform(put("/api/reports/preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deliveryChannel").value("EMAIL"));
    }

    @Test
    @DisplayName("리포트 설정 언어 수정 성공")
    void updateMyPreference_Language_Success() throws Exception {
        TestSecurityConfig.setAuthentication(parent);
        ReportPreferenceUpdateDTO updateDTO = ReportPreferenceUpdateDTO.builder()
                .language("en")
                .build();

        mockMvc.perform(put("/api/reports/preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.language").value("en"));
    }

    @Test
    @DisplayName("리포트 설정 대상 아동 지정 성공")
    void updateMyPreference_ChildScope_SpecificChild_Success() throws Exception {
        TestSecurityConfig.setAuthentication(parent);
        UUID targetChildId = UUID.randomUUID();
        ReportPreferenceUpdateDTO updateDTO = ReportPreferenceUpdateDTO.builder()
                .childScope(ReportChildScope.SPECIFIC_CHILD)
                .targetChildId(targetChildId)
                .build();

        mockMvc.perform(put("/api/reports/preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetChildId").value(targetChildId.toString()));
    }

    @Test
    @DisplayName("maxTokens가 0보다 작으면 실패한다")
    void updateMyPreference_InvalidMaxTokens_BadRequest() throws Exception {
        TestSecurityConfig.setAuthentication(parent);
        ReportPreferenceUpdateDTO updateDTO = ReportPreferenceUpdateDTO.builder()
                .maxTokens(-1)
                .build();

        mockMvc.perform(put("/api/reports/preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("cooldownHours가 음수면 실패한다")
    void updateMyPreference_NegativeCooldown_BadRequest() throws Exception {
        TestSecurityConfig.setAuthentication(parent);
        ReportPreferenceUpdateDTO updateDTO = ReportPreferenceUpdateDTO.builder()
                .cooldownHours(-1)
                .build();

        mockMvc.perform(put("/api/reports/preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("리포트 설정이 없어도 수정 요청으로 생성된다")
    void updateMyPreference_NotExists_CreatesAndUpdates() throws Exception {
        TestSecurityConfig.setAuthentication(otherUser);
        ReportPreferenceUpdateDTO updateDTO = ReportPreferenceUpdateDTO.builder()
                .enabled(true)
                .scheduleType(ReportScheduleType.MONTHLY)
                .build();

        mockMvc.perform(put("/api/reports/preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.scheduleType").value("MONTHLY"));
    }

    @Test
    @DisplayName("내 리포트 목록 조회 성공")
    void getMyReports_Success() throws Exception {
        TestSecurityConfig.setAuthentication(parent);

        mockMvc.perform(get("/api/reports/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("내 리포트 목록 페이징 조회 성공")
    void getMyReports_Paging_Success() throws Exception {
        TestSecurityConfig.setAuthentication(parent);

        mockMvc.perform(get("/api/reports/me")
                        .param("page", "0")
                        .param("size", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2));
    }

    @Test
    @DisplayName("다른 사용자의 리포트는 조회되지 않는다")
    void getMyReports_OtherUserReportsNotVisible() throws Exception {
        TestSecurityConfig.setAuthentication(otherUser);

        mockMvc.perform(get("/api/reports/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("리포트가 없어도 빈 목록으로 반환된다")
    void getMyReports_Empty_Success() throws Exception {
        TestSecurityConfig.setAuthentication(otherUser);

        mockMvc.perform(get("/api/reports/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @DisplayName("리포트 상세 조회 성공")
    void getMyReportDetail_Success() throws Exception {
        TestSecurityConfig.setAuthentication(parent);

        mockMvc.perform(get("/api/reports/{reportId}", generatedReport.getReportId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportId").value(generatedReport.getReportId().toString()))
                .andExpect(jsonPath("$.data.userId").value(parent.getUserId().toString()))
                .andExpect(jsonPath("$.data.reportBody").exists())
                .andExpect(jsonPath("$.data.issuedAt").exists());
    }

    @Test
    @DisplayName("대기 중 리포트 상세 조회 성공")
    void getMyReportDetail_Pending_Success() throws Exception {
        TestSecurityConfig.setAuthentication(parent);

        mockMvc.perform(get("/api/reports/{reportId}", pendingReport.getReportId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").doesNotExist());
    }

    @Test
    @DisplayName("다른 사용자의 리포트 상세는 조회할 수 없다")
    void getMyReportDetail_OtherUserReport_NotFound() throws Exception {
        TestSecurityConfig.setAuthentication(otherUser);

        mockMvc.perform(get("/api/reports/{reportId}", generatedReport.getReportId()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 reportId 조회는 실패한다")
    void getMyReportDetail_NotExists_NotFound() throws Exception {
        TestSecurityConfig.setAuthentication(parent);

        mockMvc.perform(get("/api/reports/{reportId}", UUID.randomUUID()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}