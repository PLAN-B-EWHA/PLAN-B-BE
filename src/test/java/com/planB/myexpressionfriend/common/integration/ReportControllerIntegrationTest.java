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
 * Report Controller ?듯빀 ?뚯뒪??
 * - GET  /api/reports/preferences/me  : 내 리포트 설정 조회 (?놁쑝硫?湲곕낯媛??앹꽦)
 * - PUT  /api/reports/preferences/me  : 내 리포트 설정 수정
 * - GET  /api/reports/me              : 내 생성 리포트 목록 조회
 * - GET  /api/reports/{reportId}      : ?앹꽦 由ы룷???곸꽭 議고쉶
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DisplayName("Report Controller ?듯빀 ?뚯뒪??)
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
        // ?ъ슜???앹꽦
        parent = User.builder()
                .email("parent@test.com")
                .password("encoded-password")
                .name("蹂댄샇??)
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(parent);

        otherUser = User.builder()
                .email("other@test.com")
                .password("encoded-password")
                .name("?ㅻⅨ?ъ슜??)
                .roles(Set.of(UserRole.PARENT))
                .build();
        userRepository.save(otherUser);

        // 由ы룷???ㅼ젙 ?앹꽦
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

        // ?앹꽦??由ы룷??
        generatedReport = GeneratedReport.builder()
                .userId(parent.getUserId())
                .preferenceId(preference.getPreferenceId())
                .status(ReportStatus.GENERATED)
                .periodStartAt(LocalDateTime.now().minusDays(7))
                .periodEndAt(LocalDateTime.now())
                .build();
        generatedReport.markGenerated(
                "二쇨컙 由ы룷??,
                "?대쾲 二??꾩씠???쒖젙 ?덈젴 ?붿빟",
                "蹂몃Ц ?댁슜?낅땲?? ?꾩씠媛 ?대쾲 二?珥?10???덈젴??완료?덉뒿?덈떎.",
                "?ъ슜???꾨＼?꾪듃",
                "gpt-4o",
                LocalDateTime.now()
        );
        reportRepository.save(generatedReport);

        // PENDING 由ы룷??
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
    @DisplayName("내 리포트 설정 조회 ?깃났 - 湲곗〈 ?ㅼ젙 諛섑솚")
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
    @DisplayName("내 리포트 설정 조회 ?깃났 - ?놁쑝硫?湲곕낯媛믪쑝濡??먮룞 ?앹꽦")
    void getMyPreference_NotExists_CreatesDefault() throws Exception {
        // given: preference媛 ?녿뒗 ?ъ슜??
        TestSecurityConfig.setAuthentication(otherUser);

        // when & then
        mockMvc.perform(get("/api/reports/preferences/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(otherUser.getUserId().toString()))
                .andExpect(jsonPath("$.data.enabled").value(false))           // 湲곕낯媛?
                .andExpect(jsonPath("$.data.scheduleType").value("WEEKLY"))   // 湲곕낯媛?
                .andExpect(jsonPath("$.data.deliveryChannel").value("IN_APP")) // 湲곕낯媛?
                .andExpect(jsonPath("$.data.language").value("ko"))           // 湲곕낯媛?
                .andExpect(jsonPath("$.data.maxTokens").value(1200));         // 湲곕낯媛?
    }

    // ============= PUT /api/reports/preferences/me =============

    @Test
    @DisplayName("由ы룷???ㅼ젙 ?섏젙 ?깃났 - enabled 蹂寃?)
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
    @DisplayName("由ы룷???ㅼ젙 ?섏젙 ?깃났 - scheduleType 蹂寃?)
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
    @DisplayName("由ы룷???ㅼ젙 ?섏젙 ?깃났 - deliveryChannel EMAIL濡?蹂寃?)
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
    @DisplayName("由ы룷???ㅼ젙 ?섏젙 ?깃났 - ?몄뼱 蹂寃?)
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
    @DisplayName("由ы룷???ㅼ젙 ?섏젙 ?깃났 - childScope SPECIFIC_CHILD濡?蹂寃?)
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
    @DisplayName("由ы룷???ㅼ젙 ?섏젙 ?ㅽ뙣 - maxTokens媛 0 ?댄븯")
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
    @DisplayName("由ы룷???ㅼ젙 ?섏젙 ?ㅽ뙣 - cooldownHours媛 ?뚯닔")
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
    @DisplayName("由ы룷???ㅼ젙 ?섏젙 ?깃났 - ?녿뒗 ?ъ슜?먮룄 湲곕낯媛??앹꽦 ???섏젙??)
    void updateMyPreference_NotExists_CreatesAndUpdates() throws Exception {
        // given: preference媛 ?녿뒗 ?ъ슜??
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
    @DisplayName("??由ы룷??紐⑸줉 議고쉶 ?깃났")
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
    @DisplayName("??由ы룷??紐⑸줉 議고쉶 ?깃났 - ?섏씠吏 ?ъ씠利??곸슜")
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
    @DisplayName("??由ы룷??紐⑸줉 議고쉶 - ?ㅻⅨ ?ъ슜?먯쓽 由ы룷?몃뒗 蹂댁씠吏 ?딅뒗??)
    void getMyReports_OtherUserReportsNotVisible() throws Exception {
        // given: 由ы룷?멸? ?녿뒗 otherUser濡?議고쉶
        TestSecurityConfig.setAuthentication(otherUser);

        // when & then
        mockMvc.perform(get("/api/reports/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("??由ы룷??紐⑸줉 議고쉶 - 由ы룷?멸? ?놁쑝硫?鍮?紐⑸줉 諛섑솚")
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
    @DisplayName("由ы룷???곸꽭 議고쉶 ?깃났")
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
                .andExpect(jsonPath("$.data.title").value("二쇨컙 由ы룷??))
                .andExpect(jsonPath("$.data.summary").value("?대쾲 二??꾩씠???쒖젙 ?덈젴 ?붿빟"))
                .andExpect(jsonPath("$.data.reportBody").exists())
                .andExpect(jsonPath("$.data.issuedAt").exists());
    }

    @Test
    @DisplayName("由ы룷???곸꽭 議고쉶 ?깃났 - PENDING ?곹깭 由ы룷??)
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
    @DisplayName("由ы룷???곸꽭 議고쉶 ?ㅽ뙣 - ?ㅻⅨ ?ъ슜?먯쓽 由ы룷??)
    void getMyReportDetail_OtherUserReport_NotFound() throws Exception {
        // given: otherUser媛 parent??由ы룷??議고쉶 ?쒕룄
        TestSecurityConfig.setAuthentication(otherUser);

        // when & then
        // IllegalArgumentException ??湲濡쒕쾶 ?덉쇅 ?몃뱾?ш? 400?쇰줈 泥섎━
        mockMvc.perform(get("/api/reports/{reportId}", generatedReport.getReportId()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("由ы룷???곸꽭 議고쉶 ?ㅽ뙣 - 議댁옱?섏? ?딅뒗 reportId")
    void getMyReportDetail_NotExists_NotFound() throws Exception {
        // given
        TestSecurityConfig.setAuthentication(parent);

        // when & then
        // IllegalArgumentException ??湲濡쒕쾶 ?덉쇅 ?몃뱾?ш? 400?쇰줈 泥섎━
        mockMvc.perform(get("/api/reports/{reportId}", UUID.randomUUID()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
