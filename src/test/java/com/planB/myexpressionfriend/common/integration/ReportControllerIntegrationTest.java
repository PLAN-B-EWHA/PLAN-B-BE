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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Report Controller 통합 테스트
 * - GET  /api/reports/preferences/me  : 내 리포트 설정 조회 (없으면 기본값 생성)
 * - PUT  /api/reports/preferences/me  : 내 리포트 설정 수정
 * - GET  /api/reports/me              : 내 생성 리포트 목록 조회
 * - GET  /api/reports/{reportId}      : 생성 리포트 상세 조회
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
        // 사용자 생성
        parent = User.builder()
                .email("parent@test.com")
                .password("encoded-password")
                .name("보호자")
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(parent);

        otherUser = User.builder()
                .email("other@test.com")
                .password("encoded-password")
                .name("다른사용자")
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(otherUser);

        // 리포트 설정 생성
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

        // 생성된 리포트
        generatedReport = GeneratedReport.builder()
                .userId(parent.getUserId())
                .preferenceId(preference.getPreferenceId())
                .status(ReportStatus.GENERATED)
                .periodStartAt(LocalDateTime.now().minusDays(7))
                .periodEndAt(LocalDateTime.now())
                .build();
        generatedReport.markGenerated(
                "주간 리포트",
                "이번 주 아이의 표정 훈련 요약",
                "본문 내용입니다. 아이가 이번 주 총 10회 훈련을 완료했습니다.",
                "사용된 프롬프트",
                "gpt-4o",
                LocalDateTime.now()
        );
        reportRepository.save(generatedReport);

        // PENDING 리포트
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

    // ============= GET /api/reports/preferences/me =============

    @Test
    @DisplayName("내 리포트 설정 조회 성공 - 기존 설정 반환")
    void getMyPreference_Exists_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(parent);

        // when & then
        mockMvc.perform(get("/api/reports/preferences/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(parent.getUserId().toString()))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.scheduleType").value("WEEKLY"))
                .andExpect(jsonPath("$.data.deliveryChannel").value("IN_APP"))
                .andExpect(jsonPath("$.data.language").value("ko"))
                .andExpect(jsonPath("$.data.modelName").value("gpt-4o"));
    }

    @Test
    @DisplayName("내 리포트 설정 조회 성공 - 없으면 기본값으로 자동 생성")
    void getMyPreference_NotExists_CreatesDefault() throws Exception {
        // given: preference가 없는 사용자
        TestSecurityConfig.setAuthentication(otherUser);

        // when & then
        mockMvc.perform(get("/api/reports/preferences/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(otherUser.getUserId().toString()))
                .andExpect(jsonPath("$.data.enabled").value(false))           // 기본값
                .andExpect(jsonPath("$.data.scheduleType").value("WEEKLY"))   // 기본값
                .andExpect(jsonPath("$.data.deliveryChannel").value("IN_APP")) // 기본값
                .andExpect(jsonPath("$.data.language").value("ko"))           // 기본값
                .andExpect(jsonPath("$.data.maxTokens").value(1200));         // 기본값
    }

    // ============= PUT /api/reports/preferences/me =============

    @Test
    @DisplayName("리포트 설정 수정 성공 - enabled 변경")
    void updateMyPreference_Enabled_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(parent);
        ReportPreferenceUpdateDTO updateDTO = ReportPreferenceUpdateDTO.builder()
                .enabled(false)
                .build();

        // when & then
        mockMvc.perform(put("/api/reports/preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    @DisplayName("리포트 설정 수정 성공 - scheduleType 변경")
    void updateMyPreference_ScheduleType_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(parent);
        ReportPreferenceUpdateDTO updateDTO = ReportPreferenceUpdateDTO.builder()
                .scheduleType(ReportScheduleType.DAILY)
                .build();

        // when & then
        mockMvc.perform(put("/api/reports/preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scheduleType").value("DAILY"));
    }

    @Test
    @DisplayName("리포트 설정 수정 성공 - deliveryChannel EMAIL로 변경")
    void updateMyPreference_DeliveryChannel_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(parent);
        ReportPreferenceUpdateDTO updateDTO = ReportPreferenceUpdateDTO.builder()
                .deliveryChannel(ReportDeliveryChannel.EMAIL)
                .build();

        // when & then
        mockMvc.perform(put("/api/reports/preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deliveryChannel").value("EMAIL"));
    }

    @Test
    @DisplayName("리포트 설정 수정 성공 - 언어 변경")
    void updateMyPreference_Language_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(parent);
        ReportPreferenceUpdateDTO updateDTO = ReportPreferenceUpdateDTO.builder()
                .language("en")
                .build();

        // when & then
        mockMvc.perform(put("/api/reports/preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.language").value("en"));
    }

    @Test
    @DisplayName("리포트 설정 수정 성공 - childScope SPECIFIC_CHILD로 변경")
    void updateMyPreference_ChildScope_SpecificChild_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(parent);
        UUID targetChildId = UUID.randomUUID();
        ReportPreferenceUpdateDTO updateDTO = ReportPreferenceUpdateDTO.builder()
                .childScope(ReportChildScope.SPECIFIC_CHILD)
                .targetChildId(targetChildId)
                .build();

        // when & then
        mockMvc.perform(put("/api/reports/preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.childScope").value("SPECIFIC_CHILD"))
                .andExpect(jsonPath("$.data.targetChildId").value(targetChildId.toString()));
    }

    @Test
    @DisplayName("리포트 설정 수정 실패 - maxTokens가 0 이하")
    void updateMyPreference_InvalidMaxTokens_BadRequest() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(parent);
        ReportPreferenceUpdateDTO updateDTO = ReportPreferenceUpdateDTO.builder()
                .maxTokens(-1)
                .build();

        // when & then
        mockMvc.perform(put("/api/reports/preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("리포트 설정 수정 실패 - cooldownHours가 음수")
    void updateMyPreference_NegativeCooldown_BadRequest() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(parent);
        ReportPreferenceUpdateDTO updateDTO = ReportPreferenceUpdateDTO.builder()
                .cooldownHours(-1)
                .build();

        // when & then
        mockMvc.perform(put("/api/reports/preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("리포트 설정 수정 성공 - 없는 사용자도 기본값 생성 후 수정됨")
    void updateMyPreference_NotExists_CreatesAndUpdates() throws Exception {
        // given: preference가 없는 사용자
        TestSecurityConfig.setAuthentication(otherUser);
        ReportPreferenceUpdateDTO updateDTO = ReportPreferenceUpdateDTO.builder()
                .enabled(true)
                .scheduleType(ReportScheduleType.MONTHLY)
                .build();

        // when & then
        mockMvc.perform(put("/api/reports/preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.scheduleType").value("MONTHLY"));
    }

    // ============= GET /api/reports/me =============

    @Test
    @DisplayName("내 리포트 목록 조회 성공")
    void getMyReports_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(parent);

        // when & then
        mockMvc.perform(get("/api/reports/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("내 리포트 목록 조회 성공 - 페이지 사이즈 적용")
    void getMyReports_Paging_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(parent);

        // when & then
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
    @DisplayName("내 리포트 목록 조회 - 다른 사용자의 리포트는 보이지 않는다")
    void getMyReports_OtherUserReportsNotVisible() throws Exception {
        // given: 리포트가 없는 otherUser로 조회
        TestSecurityConfig.setAuthentication(otherUser);

        // when & then
        mockMvc.perform(get("/api/reports/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("내 리포트 목록 조회 - 리포트가 없으면 빈 목록 반환")
    void getMyReports_Empty_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(otherUser);

        // when & then
        mockMvc.perform(get("/api/reports/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    // ============= GET /api/reports/{reportId} =============

    @Test
    @DisplayName("리포트 상세 조회 성공")
    void getMyReportDetail_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(parent);

        // when & then
        mockMvc.perform(get("/api/reports/{reportId}", generatedReport.getReportId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportId").value(generatedReport.getReportId().toString()))
                .andExpect(jsonPath("$.data.userId").value(parent.getUserId().toString()))
                .andExpect(jsonPath("$.data.status").value("GENERATED"))
                .andExpect(jsonPath("$.data.title").value("주간 리포트"))
                .andExpect(jsonPath("$.data.summary").value("이번 주 아이의 표정 훈련 요약"))
                .andExpect(jsonPath("$.data.reportBody").exists())
                .andExpect(jsonPath("$.data.issuedAt").exists());
    }

    @Test
    @DisplayName("리포트 상세 조회 성공 - PENDING 상태 리포트")
    void getMyReportDetail_Pending_Success() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(parent);

        // when & then
        mockMvc.perform(get("/api/reports/{reportId}", pendingReport.getReportId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.title").doesNotExist());
    }

    @Test
    @DisplayName("리포트 상세 조회 실패 - 다른 사용자의 리포트")
    void getMyReportDetail_OtherUserReport_NotFound() throws Exception {
        // given: otherUser가 parent의 리포트 조회 시도
        TestSecurityConfig.setAuthentication(otherUser);

        // when & then
        // IllegalArgumentException → 글로벌 예외 핸들러가 400으로 처리
        mockMvc.perform(get("/api/reports/{reportId}", generatedReport.getReportId()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("리포트 상세 조회 실패 - 존재하지 않는 reportId")
    void getMyReportDetail_NotExists_NotFound() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(parent);

        // when & then
        // IllegalArgumentException → 글로벌 예외 핸들러가 400으로 처리
        mockMvc.perform(get("/api/reports/{reportId}", UUID.randomUUID()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
