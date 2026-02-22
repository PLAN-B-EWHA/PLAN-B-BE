package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.report.GeneratedReport;
import com.planB.myexpressionfriend.common.domain.report.ReportStatus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("GeneratedReportRepository 테스트")
@Transactional
public class GeneratedReportRepositoryTest {

    @Autowired
    private GeneratedReportRepository reportRepository;

    @Autowired
    private TestEntityManager em;

    private UUID userId1;
    private UUID userId2;
    private UUID childId1;

    private GeneratedReport pendingReport;
    private GeneratedReport generatedReport;
    private GeneratedReport failedReport;
    private GeneratedReport anotherUserReport;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        childId1 = UUID.randomUUID();

        // userId1의 PENDING 리포트
        pendingReport = GeneratedReport.builder()
                .userId(userId1)
                .targetChildId(childId1)
                .status(ReportStatus.PENDING)
                .periodStartAt(LocalDateTime.now().minusDays(7))
                .periodEndAt(LocalDateTime.now())
                .build();
        em.persist(pendingReport);

        // userId1의 GENERATED 리포트
        generatedReport = GeneratedReport.builder()
                .userId(userId1)
                .targetChildId(childId1)
                .status(ReportStatus.GENERATED)
                .periodStartAt(LocalDateTime.now().minusDays(14))
                .periodEndAt(LocalDateTime.now().minusDays(7))
                .build();
        em.persist(generatedReport);

        // userId1의 FAILED 리포트
        failedReport = GeneratedReport.builder()
                .userId(userId1)
                .targetChildId(childId1)
                .status(ReportStatus.FAILED)
                .periodStartAt(LocalDateTime.now().minusDays(21))
                .periodEndAt(LocalDateTime.now().minusDays(14))
                .build();
        em.persist(failedReport);

        // userId2의 리포트 (다른 사용자)
        anotherUserReport = GeneratedReport.builder()
                .userId(userId2)
                .status(ReportStatus.GENERATED)
                .build();
        em.persist(anotherUserReport);

        em.flush();
        em.clear();
    }

    // ============= findByReportIdAndUserId 테스트 =============

    @Test
    @DisplayName("reportId와 userId로 리포트를 조회할 수 있다")
    void findByReportIdAndUserId_Success() {
        // when
        Optional<GeneratedReport> result = reportRepository.findByReportIdAndUserId(
                pendingReport.getReportId(), userId1
        );

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(result.get().getUserId()).isEqualTo(userId1);
    }

    @Test
    @DisplayName("다른 사용자의 reportId로 조회하면 빈 결과가 반환된다")
    void findByReportIdAndUserId_WrongUser_Empty() {
        // when
        Optional<GeneratedReport> result = reportRepository.findByReportIdAndUserId(
                pendingReport.getReportId(), userId2
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 reportId로 조회하면 빈 결과가 반환된다")
    void findByReportIdAndUserId_NotExistId_Empty() {
        // when
        Optional<GeneratedReport> result = reportRepository.findByReportIdAndUserId(
                UUID.randomUUID(), userId1
        );

        // then
        assertThat(result).isEmpty();
    }

    // ============= findByUserIdOrderByCreatedAtDesc 테스트 =============

    @Test
    @DisplayName("userId로 리포트 목록을 최신순으로 페이징 조회할 수 있다")
    void findByUserIdOrderByCreatedAtDesc_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<GeneratedReport> result = reportRepository.findByUserIdOrderByCreatedAtDesc(
                userId1, pageRequest
        );

        // then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).allMatch(r -> r.getUserId().equals(userId1));
    }

    @Test
    @DisplayName("페이지 사이즈보다 결과가 많으면 페이징이 적용된다")
    void findByUserIdOrderByCreatedAtDesc_Paging() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 2);

        // when
        Page<GeneratedReport> result = reportRepository.findByUserIdOrderByCreatedAtDesc(
                userId1, pageRequest
        );

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("리포트가 없는 사용자는 빈 페이지가 반환된다")
    void findByUserIdOrderByCreatedAtDesc_NoReport_EmptyPage() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);
        UUID unknownUser = UUID.randomUUID();

        // when
        Page<GeneratedReport> result = reportRepository.findByUserIdOrderByCreatedAtDesc(
                unknownUser, pageRequest
        );

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ============= countByUserIdAndStatus 테스트 =============

    @Test
    @DisplayName("userId와 status로 리포트 개수를 조회할 수 있다")
    void countByUserIdAndStatus_Success() {
        // when
        long pendingCount = reportRepository.countByUserIdAndStatus(userId1, ReportStatus.PENDING);
        long generatedCount = reportRepository.countByUserIdAndStatus(userId1, ReportStatus.GENERATED);
        long failedCount = reportRepository.countByUserIdAndStatus(userId1, ReportStatus.FAILED);

        // then
        assertThat(pendingCount).isEqualTo(1);
        assertThat(generatedCount).isEqualTo(1);
        assertThat(failedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("해당 상태의 리포트가 없으면 0을 반환한다")
    void countByUserIdAndStatus_NoMatch_Zero() {
        // when
        long count = reportRepository.countByUserIdAndStatus(userId1, ReportStatus.SKIPPED);

        // then
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("다른 사용자의 리포트는 카운트에 포함되지 않는다")
    void countByUserIdAndStatus_OtherUser_NotCounted() {
        // when
        long count = reportRepository.countByUserIdAndStatus(userId2, ReportStatus.PENDING);

        // then
        assertThat(count).isZero();
    }

    // ============= findByStatusAndCreatedAtAfter 테스트 =============

    @Test
    @DisplayName("특정 상태이고 특정 시간 이후에 생성된 리포트를 조회할 수 있다")
    void findByStatusAndCreatedAtAfter_Success() {
        // given
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<GeneratedReport> result = reportRepository.findByStatusAndCreatedAtAfter(
                ReportStatus.GENERATED, oneDayAgo, pageRequest
        );

        // then
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent()).allMatch(r -> r.getStatus() == ReportStatus.GENERATED);
    }

    @Test
    @DisplayName("미래 기준 시간으로 조회하면 빈 결과가 반환된다")
    void findByStatusAndCreatedAtAfter_FutureDate_Empty() {
        // given
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<GeneratedReport> result = reportRepository.findByStatusAndCreatedAtAfter(
                ReportStatus.GENERATED, future, pageRequest
        );

        // then
        assertThat(result.getContent()).isEmpty();
    }

    // ============= 도메인 메서드 연동 테스트 =============

    @Test
    @DisplayName("markGenerated 후 저장하면 상태가 GENERATED로 변경된다")
    void markGenerated_StatusChangedAndPersisted() {
        // given
        GeneratedReport report = reportRepository.findById(pendingReport.getReportId()).orElseThrow();

        // when
        report.markGenerated("주간 리포트", "요약 내용", "본문 내용", "사용된 프롬프트", "gpt-4o", LocalDateTime.now());
        reportRepository.save(report);
        em.flush();
        em.clear();

        GeneratedReport saved = reportRepository.findById(pendingReport.getReportId()).orElseThrow();

        // then
        assertThat(saved.getStatus()).isEqualTo(ReportStatus.GENERATED);
        assertThat(saved.getTitle()).isEqualTo("주간 리포트");
        assertThat(saved.getReportBody()).isEqualTo("본문 내용");
        assertThat(saved.getIssuedAt()).isNotNull();
        assertThat(saved.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("markFailed 후 저장하면 상태가 FAILED로 변경되고 실패 사유가 저장된다")
    void markFailed_StatusChangedAndPersisted() {
        // given
        GeneratedReport report = reportRepository.findById(pendingReport.getReportId()).orElseThrow();

        // when
        report.markFailed("AI API 호출 실패");
        reportRepository.save(report);
        em.flush();
        em.clear();

        GeneratedReport saved = reportRepository.findById(pendingReport.getReportId()).orElseThrow();

        // then
        assertThat(saved.getStatus()).isEqualTo(ReportStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo("AI API 호출 실패");
    }

    @Test
    @DisplayName("markSkipped 후 저장하면 상태가 SKIPPED로 변경된다")
    void markSkipped_StatusChangedAndPersisted() {
        // given
        GeneratedReport report = reportRepository.findById(pendingReport.getReportId()).orElseThrow();

        // when
        report.markSkipped("데이터 없음");
        reportRepository.save(report);
        em.flush();
        em.clear();

        GeneratedReport saved = reportRepository.findById(pendingReport.getReportId()).orElseThrow();

        // then
        assertThat(saved.getStatus()).isEqualTo(ReportStatus.SKIPPED);
        assertThat(saved.getFailureReason()).isEqualTo("데이터 없음");
    }
}
