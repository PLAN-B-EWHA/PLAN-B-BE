package com.planB.myexpressionfriend.unity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Unity 미션 목록 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityMissionListResponseDTO {

    private List<UnityMissionResponseDTO> missions;
}