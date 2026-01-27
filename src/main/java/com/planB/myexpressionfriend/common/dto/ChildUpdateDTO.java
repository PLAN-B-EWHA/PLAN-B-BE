package com.planB.myexpressionfriend.common.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 아동 정보 수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildUpdateDTO {

    @Size(min = 2, max = 50, message = "이름은 2-50자 사이여야 합니다")
    private String name;

    @Past(message = "생년월일은 과거 날짜여야 합니다")
    private LocalDate birthDate;

    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "성별은 MALE, FEMALE, OTHER 중 하나여야 합니다")
    private String gender;

    @Past(message = "진단일은 과거 날짜여야 합니다")
    private LocalDate diagnosisDate;
}