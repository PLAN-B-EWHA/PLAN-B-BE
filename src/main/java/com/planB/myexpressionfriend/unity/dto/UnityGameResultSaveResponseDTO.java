package com.planB.myexpressionfriend.unity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Unity 게임 결과 저장 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityGameResultSaveResponseDTO {

    private Long savedId;
    private Integer missionId;
    private LocalDateTime createdAt;
}