package myexpressionfriend_api.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import myexpressionfriend_api.auth.domain.user.UserRole;

@Getter
@NoArgsConstructor
public class AdminUserRoleUpdateDTO {

    @NotNull(message = "변경할 역할은 필수입니다.")
    private UserRole role;
}
