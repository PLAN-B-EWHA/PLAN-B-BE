package com.planB.myexpressionfriend.unity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Unity 미션 가져오기 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnityMissionImportRequestDTO {

    @Valid
    @NotEmpty
    private List<UnityMissionItemDTO> missions;
}