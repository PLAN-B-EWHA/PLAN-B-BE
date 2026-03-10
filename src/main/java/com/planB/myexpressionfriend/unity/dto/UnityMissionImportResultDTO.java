package com.planB.myexpressionfriend.unity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Unity 미션 가져오기 결과 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityMissionImportResultDTO {

    private int requestedCount;
    private int savedCount;
    private List<Long> savedIds;
}