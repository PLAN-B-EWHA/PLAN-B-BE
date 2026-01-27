package com.planB.myexpressionfriend.common.dto;

import com.planB.myexpressionfriend.common.domain.Child;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 아동 상세 정보 DTO (권한 목록 포함)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildDetailDTO {

    private UUID childId;
    private String name;
    private LocalDate birthDate;
    private String gender;
    private LocalDate diagnosisDate;
    private Boolean pinEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 주보호자 정보
     */
    private UserBasicDTO primaryParent;

    /**
     * 권한 부여된 사용자 목록
     */
    private List<AuthorizedUserDTO> authorizedUsers;

    /**
     * Entity → DTO 변환
     */
    public static ChildDetailDTO from(Child child) {
        // 주보호자 찾기
        UserBasicDTO primaryParent = child.getPrimaryParent()
                .map(UserBasicDTO::from)
                .orElse(null);

        // 권한 목록 변환
        List<AuthorizedUserDTO> authorizedUsers = child.getAuthorizedUsers().stream()
                .filter(au -> au.getIsActive())
                .map(AuthorizedUserDTO::from)
                .collect(Collectors.toList());

        return ChildDetailDTO.builder()
                .childId(child.getChildId())
                .name(child.getName())
                .birthDate(child.getBirthDate())
                .gender(child.getGender())
                .diagnosisDate(child.getDiagnosisDate())
                .pinEnabled(child.getPinEnabled())
                .createdAt(child.getCreatedAt())
                .updatedAt(child.getUpdatedAt())
                .primaryParent(primaryParent)
                .authorizedUsers(authorizedUsers)
                .build();
    }
}