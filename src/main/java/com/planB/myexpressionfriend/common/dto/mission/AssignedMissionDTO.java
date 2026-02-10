package com.planB.myexpressionfriend.common.dto.mission;

import com.planB.myexpressionfriend.common.domain.mission.AssignedMission;
import com.planB.myexpressionfriend.common.domain.mission.MissionStatus;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 할당된 미션 응답 DTO (목록용)
 */
@Getter
@Builder
public class AssignedMissionDTO {

    private UUID missionId;
    private UUID childId;
    private String childName;
    private UserDTO therapist;
    private MissionTemplateDTO template;
    private MissionStatus status;
    private LocalDateTime assignedAt;
    private LocalDateTime dueDate;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime verifiedAt;
    private Boolean isOverdue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static AssignedMissionDTO from(AssignedMission mission) {
        if (mission == null) {
            return null;
        }

        return AssignedMissionDTO.builder()
                .missionId(mission.getMissionId())
                .childId(mission.getChild().getChildId())
                .childName(mission.getChild().getName())
                .therapist(UserDTO.from(mission.getTherapist()))
                .template(MissionTemplateDTO.from(mission.getTemplate()))
                .status(mission.getStatus())
                .assignedAt(mission.getAssignedAt())
                .dueDate(mission.getDueDate())
                .startedAt(mission.getStartedAt())
                .completedAt(mission.getCompletedAt())
                .verifiedAt(mission.getVerifiedAt())
                .isOverdue(mission.isOverdue())
                .createdAt(mission.getCreatedAt())
                .updatedAt(mission.getUpdatedAt())
                .build();
    }
}