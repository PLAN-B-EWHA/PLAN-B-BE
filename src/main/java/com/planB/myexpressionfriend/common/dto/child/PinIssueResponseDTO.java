package com.planB.myexpressionfriend.common.dto.child;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Temporary PIN issue response DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinIssueResponseDTO {

    /**
     * Newly issued 4-digit PIN (shown once at issue time)
     */
    private String pin;
}

