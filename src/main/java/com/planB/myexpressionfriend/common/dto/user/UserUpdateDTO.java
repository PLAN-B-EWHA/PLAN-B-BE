package com.planB.myexpressionfriend.common.dto.user;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateDTO {

    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters.")
    private String name;

    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters.")
    private String password;
}

