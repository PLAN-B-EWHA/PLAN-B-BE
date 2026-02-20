package com.planB.myexpressionfriend.common.dto.user;

import com.planB.myexpressionfriend.common.domain.user.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleUpdateDTO {

    @NotNull(message = "Role is required")
    private UserRole role;
}
