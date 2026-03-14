package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.report.ReportChildScope;
import com.planB.myexpressionfriend.common.domain.report.ReportDeliveryChannel;
import com.planB.myexpressionfriend.common.domain.report.ReportPreference;
import com.planB.myexpressionfriend.common.domain.report.ReportScheduleType;
import com.planB.myexpressionfriend.common.repository.ReportPreferenceRepository;
import com.planB.myexpressionfriend.common.exception.EntityNotFoundException;
import com.planB.myexpressionfriend.common.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportPreferenceService {

    private final ReportPreferenceRepository reportPreferenceRepository;
    private final ChildAuthorizationService childAuthorizationService;

    @Transactional
    public ReportPreference getOrCreate(UUID userId) {
        return reportPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefault(userId));
    }

    public ReportPreference getByUserId(UUID userId) {
        return reportPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("리포트 설정을 찾을 수 없습니다."));
    }

    public List<ReportPreference> findIssuablePreferences(LocalDateTime now) {
        return reportPreferenceRepository.findByEnabledTrueAndNextIssueAtLessThanEqual(now);
    }

    @Transactional
    public ReportPreference updatePreference(
            UUID userId,
            Boolean enabled,
            ReportScheduleType scheduleType,
            LocalTime deliveryTime,
            String timezone,
            ReportDeliveryChannel deliveryChannel,
            ReportChildScope childScope,
            UUID targetChildId,
            String language,
            String modelName,
            String promptTemplate,
            Integer maxTokens,
            Boolean autoIssueOnNoData,
            Integer cooldownHours
    ) {
        ReportPreference preference = getOrCreate(userId);

        if (deliveryChannel != null) {
            preference.changeDeliveryChannel(deliveryChannel);
        }

        if (scheduleType != null || deliveryTime != null || timezone != null) {
            preference.updateDelivery(
                    scheduleType != null ? scheduleType : preference.getScheduleType(),
                    deliveryTime != null ? deliveryTime : preference.getDeliveryTime(),
                    timezone != null ? timezone : preference.getTimezone()
            );
        }

        if (childScope != null || targetChildId != null) {
            preference.updateChildScope(
                    childScope != null ? childScope : preference.getChildScope(),
                    targetChildId != null ? targetChildId : preference.getTargetChildId()
            );
        }

        if (preference.getTargetChildId() != null) {
            validateViewReportPermission(userId, preference.getTargetChildId());
        }

        if (modelName != null || promptTemplate != null || maxTokens != null) {
            preference.updatePrompt(
                    promptTemplate != null ? promptTemplate : preference.getPromptTemplate(),
                    modelName != null ? modelName : preference.getModelName(),
                    maxTokens != null ? maxTokens : preference.getMaxTokens()
            );
        }

        if (autoIssueOnNoData != null || cooldownHours != null) {
            preference.updateIssuePolicy(
                    autoIssueOnNoData != null ? autoIssueOnNoData : preference.getAutoIssueOnNoData(),
                    cooldownHours != null ? cooldownHours : preference.getCooldownHours()
            );
        }

        if (language != null && !language.isBlank()) {
            preference.changeLanguage(language.trim());
        }

        if (enabled != null) {
            if (enabled) {
                preference.enable();
                if (preference.getNextIssueAt() == null) {
                    preference.updateNextIssueAt(LocalDateTime.now());
                }
            } else {
                preference.disable();
            }
        }

        if (Boolean.TRUE.equals(preference.getEnabled()) && preference.getNextIssueAt() == null) {
            preference.updateNextIssueAt(LocalDateTime.now());
        }

        if (Boolean.TRUE.equals(preference.getEnabled()) && preference.getTargetChildId() == null) {
            throw new IllegalStateException("자동 리포트 발행에는 targetChildId가 필요합니다.");
        }

        ReportPreference saved = reportPreferenceRepository.save(preference);
        log.info("Report preference updated. userId={}, enabled={}, scheduleType={}, nextIssueAt={}",
                saved.getUserId(), saved.getEnabled(), saved.getScheduleType(), saved.getNextIssueAt());
        return saved;
    }

    @Transactional
    public void markIssued(UUID userId, LocalDateTime issuedAt, LocalDateTime nextIssueAt) {
        ReportPreference preference = getByUserId(userId);
        preference.markIssued(issuedAt, nextIssueAt);
        reportPreferenceRepository.save(preference);
    }

    public LocalDateTime calculateNextIssueAt(ReportPreference preference, LocalDateTime issuedAt) {
        if (preference == null) {
            throw new InvalidRequestException("리포트 설정 정보가 필요합니다.");
        }
        LocalDateTime baseTime = issuedAt != null ? issuedAt : LocalDateTime.now();

        ZoneId zoneId = ZoneId.of(preference.getTimezone());
        ZonedDateTime zoned = baseTime.atZone(zoneId)
                .withHour(preference.getDeliveryTime().getHour())
                .withMinute(preference.getDeliveryTime().getMinute())
                .withSecond(preference.getDeliveryTime().getSecond())
                .withNano(0);

        switch (preference.getScheduleType()) {
            case DAILY -> zoned = zoned.plusDays(1);
            case WEEKLY -> zoned = zoned.plusWeeks(1);
            case MONTHLY -> zoned = zoned.plusMonths(1);
            default -> zoned = zoned.plusWeeks(1);
        }
        return zoned.toLocalDateTime();
    }

    @Transactional
    public void setEnabled(UUID userId, boolean enabled) {
        ReportPreference preference = getOrCreate(userId);
        if (enabled) {
            preference.enable();
            if (preference.getNextIssueAt() == null) {
                preference.updateNextIssueAt(LocalDateTime.now());
            }
        } else {
            preference.disable();
        }
        reportPreferenceRepository.save(preference);
    }

    @Transactional
    public void postponeNextIssue(UUID userId, LocalDateTime nextIssueAt) {
        ReportPreference preference = getByUserId(userId);
        preference.updateNextIssueAt(nextIssueAt);
        reportPreferenceRepository.save(preference);
    }

    @Transactional
    private ReportPreference createDefault(UUID userId) {
        ReportPreference created = reportPreferenceRepository.save(
                ReportPreference.builder()
                        .userId(userId)
                        .build()
        );
        log.info("Default report preference created. userId={}", userId);
        return created;
    }

    private void validateViewReportPermission(UUID userId, UUID childId) {
        boolean hasPermission = childAuthorizationService.hasPermission(childId, userId, ChildPermissionType.VIEW_REPORT);
        if (!hasPermission) {
            throw new org.springframework.security.access.AccessDeniedException("리포트 조회 권한(VIEW_REPORT)이 없습니다.");
        }
    }
}
