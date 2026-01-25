package com.planB.myexpressionfriend.common.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {

    PARENT("ROLE_PARENT", "학부모"),
    THERAPIST("ROLE_THERAPIST", "치료사"),
    TEACHER("ROLE_TEACHER", "교사"),
    ADMIN("ROLE_ADMIN", "관리자");

    private final String key;
    private final String description;


    // 메서드

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public static UserRole of(String roleName) {
        for (UserRole role : values()) {
            if (role.name().equalsIgnoreCase(roleName)) {
                return role;
            }
        }
        throw new IllegalArgumentException("해당하는 권한이 없습니다: " + roleName);
    }
}
