package com.planB.myexpressionfriend.common.dto.child;

import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * Child authorization request DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildAuthorizationDTO {

    @NotNull(message = "userId is required.")
    private UUID userId;

    @NotEmpty(message = "At least one permission is required.")
    private Set<ChildPermissionType> permissions;

    private Boolean isPrimary;
}

