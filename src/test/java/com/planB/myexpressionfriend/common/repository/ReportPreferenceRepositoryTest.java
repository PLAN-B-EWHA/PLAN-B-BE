package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.report.ReportChildScope;
import com.planB.myexpressionfriend.common.domain.report.ReportDeliveryChannel;
import com.planB.myexpressionfriend.common.domain.report.ReportPreference;
import com.planB.myexpressionfriend.common.domain.report.ReportScheduleType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace =  AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ReportPreferenceRepository Test")
@Transactional
public class ReportPreferenceRepositoryTest {

    @Autowired
    private ReportPreferenceRepository preferenceRepository;

    @Autowired
    private TestEntityManager em;

    private UUID userId1;
    private UUID userId2;
    private UUID userId3;

    private ReportPreference enabledPreference;
    private ReportPreference disabledPreference;
    private ReportPreference issuablePreference;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        userId3 = UUID.randomUUID();

        // 활성화된 preference (nextIssueAt이 과거 → 발행 대상)
        enabledPreference = ReportPreference.builder()
                .userId(userId1)
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
        em.persist(enabledPreference);
        // nextIssueAt 직접 세팅 (과거 시간)
        em.getEntityManager()
                .createQuery("UPDATE ReportPreference p SET p.nextIssueAt = :t WHERE p.preferenceId = :id")
                .setParameter("t", LocalDateTime.now().minusHours(1))
                .setParameter("id", enabledPreference.getPreferenceId())
                .executeUpdate();

        // 비활성화된 preference
        disabledPreference = ReportPreference.builder()
                .userId(userId2)
                .enabled(false)
                .scheduleType(ReportScheduleType.DAILY)
                .deliveryChannel(ReportDeliveryChannel.EMAIL)
                .deliveryTime(LocalTime.of(8, 0))
                .timezone("Asia/Seoul")
                .childScope(ReportChildScope.ALL_CHILDREN)
                .language("ko")
                .modelName("gpt-4o")
                .maxTokens(1000)
                .cooldownHours(12)
                .autoIssueOnNoData(false)
                .build();
        em.persist(disabledPreference);

        // 활성화 + nextIssueAt이 미래 (아직 발행 대상 아님)
        issuablePreference = ReportPreference.builder()
                .userId(userId3)
                .enabled(true)
                .scheduleType(ReportScheduleType.MONTHLY)
                .deliveryChannel(ReportDeliveryChannel.IN_APP)
                .deliveryTime(LocalTime.of(10, 0))
                .timezone("Asia/Seoul")
                .childScope(ReportChildScope.ALL_CHILDREN)
                .language("ko")
                .modelName("gpt-4o")
                .maxTokens(2000)
                .cooldownHours(48)
                .autoIssueOnNoData(false)
                .build();
        em.persist(issuablePreference);
        em.getEntityManager()
                .createQuery("UPDATE ReportPreference p SET p.nextIssueAt = :t WHERE p.preferenceId = :id")
                .setParameter("t", LocalDateTime.now().plusDays(7))
                .setParameter("id", issuablePreference.getPreferenceId())
                .executeUpdate();

        em.flush();
        em.clear();
    }

    // ============= findByUserId 테스트 =============

    @Test
    @DisplayName("userId로 preference를 조회할 수 있다")
    void findByUserId_Success() {
        // when
        Optional<ReportPreference> result = preferenceRepository.findByUserId(userId1);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(userId1);
        assertThat(result.get().getEnabled()).isTrue();
        assertThat(result.get().getScheduleType()).isEqualTo(ReportScheduleType.WEEKLY);
    }

    @Test
    @DisplayName("존재하지 않는 userId로 조회하면 빈 결과가 반환된다")
    void findByUserId_NotExist_Empty() {
        // when
        Optional<ReportPreference> result = preferenceRepository.findByUserId(UUID.randomUUID());

        // then
        assertThat(result).isEmpty();
    }

    // ============= existsByUserId 테스트 =============

    @Test
    @DisplayName("userId에 해당하는 preference가 존재하면 true를 반환한다")
    void existByUserId_Exists_True() {
        // when
        boolean exists = preferenceRepository.existsByUserId(userId1);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("userId에 해당하는 preference가 없으면 false를 반환한다")
    void existsByUserId_NotExists_False() {
        // when
        boolean exists = preferenceRepository.existsByUserId(UUID.randomUUID());

        // then
        assertThat(exists).isFalse();
    }

    // ============= findByEnabledTrueAndNextIssueAtLessThanEqual 테스트 =============

    @Test
    @DisplayName("활성화 상태이고 nextIssueAt이 현재 이전인 preference만 반환된다")
    void findByEnabledTrueNextIssueAtLessThanEqual_OnlyIssuable() {

        // when
        List<ReportPreference> result = preferenceRepository.findByEnabledTrueAndNextIssueAtLessThanEqual(LocalDateTime.now());

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(userId1);
    }

    @Test
    @DisplayName("비활성화된 preference는 결과에 포함되지 않는다")
    void findByEnabledTrueAndNextIssueAtLessThanEqual_DisabledExcluded() {
        // when
        List<ReportPreference> result =  preferenceRepository.findByEnabledTrueAndNextIssueAtLessThanEqual(LocalDateTime.now());

        // then
        assertThat(result).noneMatch(p -> p.getUserId().equals(userId2));
    }

    @Test
    @DisplayName("nextIssueAt이 미래인 preference는 결과에 포함되지 않는다")
    void findByEnabledTrueAndNextIssueAtLessThanEqual_FutureNextIssueExcluded() {
        // when
        List<ReportPreference> result = preferenceRepository.findByEnabledTrueAndNextIssueAtLessThanEqual(
                LocalDateTime.now()
        );

        // then
        assertThat(result).noneMatch(p -> p.getUserId().equals(userId3));
    }

    @Test
    @DisplayName("발행 대상이 없으면 빈 리스트가 반환된다")
    void findByEnabledTrueAndNextIssueAtLessThanEqual_NoneMatch_Empty() {
        // given: 과거 시간으로 조회하면 아무것도 해당 안됨
        LocalDateTime veryOldTime = LocalDateTime.now().minusYears(10);

        // when
        List<ReportPreference> result = preferenceRepository.findByEnabledTrueAndNextIssueAtLessThanEqual(
                veryOldTime
        );

        // then
        assertThat(result).isEmpty();
    }

    // ============= 도메인 메서드 연동 테스트 =============

    @Test
    @DisplayName("enable() 후 저장하면 enabled가 true로 변경된다")
    void enable_SavedAndPermission() {
        // given
        ReportPreference preference = preferenceRepository.findByUserId(userId2).orElseThrow();
        assertThat(preference.getEnabled()).isFalse();

        // when
        preference.enable();
        preferenceRepository.save(preference);
        em.flush();
        em.clear();

        ReportPreference saved = preferenceRepository.findByUserId(userId2).orElseThrow();

        // then
        assertThat(saved.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("disable() 후 저장하면 enabled가 false로 변경된다")
    void disable_SavedAndPersisted() {
        // given
        ReportPreference preference = preferenceRepository.findByUserId(userId1).orElseThrow();
        assertThat(preference.getEnabled()).isTrue();

        // when
        preference.disable();
        preferenceRepository.save(preference);
        em.flush();
        em.clear();

        ReportPreference saved = preferenceRepository.findByUserId(userId1).orElseThrow();

        // then
        assertThat(saved.getEnabled()).isFalse();
    }

    @Test
    @DisplayName("markIssued() 후 저장하면 lastIssuedAt과 nextIssueAt이 갱신된다")
    void markIssued_SavedAndPersisted() {
        // given
        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime nextIssueAt = issuedAt.plusWeeks(1);
        ReportPreference preference = preferenceRepository.findByUserId(userId1).orElseThrow();

        // when
        preference.markIssued(issuedAt, nextIssueAt);
        preferenceRepository.save(preference);
        em.flush();
        em.clear();

        ReportPreference saved = preferenceRepository.findByUserId(userId1).orElseThrow();

        // then
        assertThat(saved.getLastIssuedAt()).isNotNull();
        assertThat(saved.getNextIssueAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("updateDelivery() 후 저장하면 스케줄 정보가 변경된다")
    void updateDelivery_SavedAndPersisted() {
        // given
        ReportPreference preference = preferenceRepository.findByUserId(userId1).orElseThrow();

        // when
        preference.updateDelivery(ReportScheduleType.MONTHLY, LocalTime.of(22, 0), "UTC");
        preferenceRepository.save(preference);
        em.flush();
        em.clear();

        ReportPreference saved = preferenceRepository.findByUserId(userId1).orElseThrow();

        // then
        assertThat(saved.getScheduleType()).isEqualTo(ReportScheduleType.MONTHLY);
        assertThat(saved.getDeliveryTime()).isEqualTo(LocalTime.of(22, 0));
        assertThat(saved.getTimezone()).isEqualTo("UTC");
    }

    @Test
    @DisplayName("updateChildScope(SPECIFIC_CHILD) 저장 시 targetChildId가 함께 저장된다")
    void updateChildScope_SpecificChild_Persisted() {
        // given
        UUID targetChild = UUID.randomUUID();
        ReportPreference preference = preferenceRepository.findByUserId(userId1).orElseThrow();

        // when
        preference.updateChildScope(ReportChildScope.SPECIFIC_CHILD, targetChild);
        preferenceRepository.save(preference);
        em.flush();
        em.clear();

        ReportPreference saved = preferenceRepository.findByUserId(userId1).orElseThrow();

        // then
        assertThat(saved.getChildScope()).isEqualTo(ReportChildScope.SPECIFIC_CHILD);
        assertThat(saved.getTargetChildId()).isEqualTo(targetChild);
    }

    @Test
    @DisplayName("updateChildScope(ALL_CHILDREN) 저장 시 targetChildId가 null로 초기화된다")
    void updateChildScope_AllChildren_TargetChildIdNull() {
        // given: 먼저 SPECIFIC_CHILD로 설정
        UUID targetChild = UUID.randomUUID();
        ReportPreference preference = preferenceRepository.findByUserId(userId1).orElseThrow();
        preference.updateChildScope(ReportChildScope.SPECIFIC_CHILD, targetChild);
        preferenceRepository.save(preference);
        em.flush();
        em.clear();

        // when: ALL_CHILDREN으로 변경
        ReportPreference updated = preferenceRepository.findByUserId(userId1).orElseThrow();
        updated.updateChildScope(ReportChildScope.ALL_CHILDREN, null);
        preferenceRepository.save(updated);
        em.flush();
        em.clear();

        ReportPreference saved = preferenceRepository.findByUserId(userId1).orElseThrow();

        // then
        assertThat(saved.getChildScope()).isEqualTo(ReportChildScope.ALL_CHILDREN);
        assertThat(saved.getTargetChildId()).isNull();
    }

    // ============= 유니크 제약 테스트 =============

    @Test
    @DisplayName("동일한 userId로 preference를 중복 저장하면 예외가 발생한다")
    void save_DuplicateUserId_ThrowsException() {
        // given
        ReportPreference duplicate = ReportPreference.builder()
                .userId(userId1) // 이미 존재하는 userId
                .build();
        em.persist(duplicate);

        // when & then
        assertThatThrownBy(() -> em.flush())
                .isInstanceOf(Exception.class); // DataIntegrityViolationException 등
    }
}
