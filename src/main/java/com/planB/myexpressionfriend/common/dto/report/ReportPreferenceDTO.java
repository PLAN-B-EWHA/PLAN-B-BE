package com.planB.myexpressionfriend.common.dto.report;

import com.planB.myexpressionfriend.common.domain.report.ReportChildScope;
import com.planB.myexpressionfriend.common.domain.report.ReportDeliveryChannel;
import com.planB.myexpressionfriend.common.domain.report.ReportPreference;
import com.planB.myexpressionfriend.common.domain.report.ReportScheduleType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Builder
public class ReportPreferenceDTO {

    private UUID preferenceId;
    private UUID userId;
    private Boolean enabled;
    private ReportScheduleType scheduleType;
    private ReportDeliveryChannel deliveryChannel;
    private LocalTime deliveryTime;
    private String timezone;
    private ReportChildScope childScope;
    private UUID targetChildId;
    private String language;
    private String modelName;
    private String promptTemplate;
    private Integer maxTokens;
    private Boolean autoIssueOnNoData;
    private Integer cooldownHours;
    private LocalDateTime lastIssuedAt;
    private LocalDateTime nextIssueAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReportPreferenceDTO from(ReportPreference preference) {
        return ReportPreferenceDTO.builder()
                .preferenceId(preference.getPreferenceId())
                .userId(preference.getUserId())
                .enabled(preference.getEnabled())
                .scheduleType(preference.getScheduleType())
                .deliveryChannel(preference.getDeliveryChannel())
                .deliveryTime(preference.getDeliveryTime())
                .timezone(preference.getTimezone())
                .childScope(preference.getChildScope())
                .targetChildId(preference.getTargetChildId())
                .language(preference.getLanguage())
                .modelName(preference.getModelName())
                .promptTemplate(preference.getPromptTemplate())
                .maxTokens(preference.getMaxTokens())
                .autoIssueOnNoData(preference.getAutoIssueOnNoData())
                .cooldownHours(preference.getCooldownHours())
                .lastIssuedAt(preference.getLastIssuedAt())
                .nextIssueAt(preference.getNextIssueAt())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
}
