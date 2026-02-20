package com.planB.myexpressionfriend.common.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {

    PENDING("ROLE_PENDING", "PENDING"),
    PARENT("ROLE_PARENT", "PARENT"),
    THERAPIST("ROLE_THERAPIST", "THERAPIST"),
    TEACHER("ROLE_TEACHER", "TEACHER"),
    ADMIN("ROLE_ADMIN", "ADMIN");

    private final String key;
    private final String description;

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public static UserRole of(String roleName) {
        for (UserRole role : values()) {
            if (role.name().equalsIgnoreCase(roleName)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unsupported role: " + roleName);
    }
}
