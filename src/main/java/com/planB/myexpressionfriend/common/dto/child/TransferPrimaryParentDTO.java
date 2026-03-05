package com.planB.myexpressionfriend.common.dto.child;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Transfer primary parent request DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferPrimaryParentDTO {

    @NotNull(message = "New primary parent userId is required.")
    private UUID newPrimaryParentUserId;

    @NotBlank(message = "PIN is required.")
    private String pin;
}

