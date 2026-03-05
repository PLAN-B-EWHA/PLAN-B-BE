package com.planB.myexpressionfriend.common.dto.child;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PIN verification request DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinVerificationDTO {

    @NotBlank(message = "PIN is required.")
    @Pattern(regexp = "^\\d{4}$", message = "PIN must be 4 digits.")
    private String pin;
}

