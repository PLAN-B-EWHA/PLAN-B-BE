package com.planB.myexpressionfriend.common.dto.child;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PIN set/change request DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinUpdateDTO {

    /**
     * Current PIN (required only when PIN already exists)
     */
    @Pattern(regexp = "^\\d{4}$", message = "Current PIN must be 4 digits.")
    private String currentPin;

    /**
     * New PIN
     */
    @NotBlank(message = "New PIN is required.")
    @Pattern(regexp = "^\\d{4}$", message = "New PIN must be 4 digits.")
    private String newPin;
}

