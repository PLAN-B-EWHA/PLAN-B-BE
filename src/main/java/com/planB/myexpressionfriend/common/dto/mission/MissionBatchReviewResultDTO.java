package com.planB.myexpressionfriend.common.dto.mission;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class MissionBatchReviewResultDTO {

    private int requestedCount;
    private int successCount;
    private int failureCount;
    private List<UUID> succeededMissionIds;
    private List<FailureItem> failures;

    public static MissionBatchReviewResultDTO empty() {
        return MissionBatchReviewResultDTO.builder()
                .requestedCount(0)
                .successCount(0)
                .failureCount(0)
                .succeededMissionIds(new ArrayList<>())
                .failures(new ArrayList<>())
                .build();
    }

    @Getter
    @Builder
    public static class FailureItem {
        private UUID missionId;
        private String reason;
    }
}

