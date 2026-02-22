package com.planB.myexpressionfriend.common.dto.report;

import com.planB.myexpressionfriend.common.domain.report.ReportChildScope;
import com.planB.myexpressionfriend.common.domain.report.ReportDeliveryChannel;
import com.planB.myexpressionfriend.common.domain.report.ReportScheduleType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportPreferenceUpdateDTO {

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

    @Positive(message = "maxTokens must be positive")
    private Integer maxTokens;

    private Boolean autoIssueOnNoData;

    @PositiveOrZero(message = "cooldownHours must be zero or positive")
    private Integer cooldownHours;
}
