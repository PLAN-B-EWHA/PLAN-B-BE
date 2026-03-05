package com.planB.myexpressionfriend.common.dto.mission;

import com.planB.myexpressionfriend.common.domain.mission.AssignedMission;
import com.planB.myexpressionfriend.common.domain.mission.MissionStatus;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 */
@Getter
@Builder
public class AssignedMissionDetailDTO {

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
    private String parentNote;
    private String therapistFeedback;
    private List<MissionPhotoDTO> photos;
    private UUID systemNoteId;
    private Boolean isOverdue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     */
    public static AssignedMissionDetailDTO from(AssignedMission mission) {
        if (mission == null) {
            return null;
        }

        return AssignedMissionDetailDTO.builder()
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
                .parentNote(mission.getParentNote())
                .therapistFeedback(mission.getTherapistFeedback())
                .photos(mission.getPhotos().stream()
                        .map(MissionPhotoDTO::from)
                        .collect(Collectors.toList()))
                .systemNoteId(mission.getSystemNote() != null
                        ? mission.getSystemNote().getNoteId()
                        : null)
                .isOverdue(mission.isOverdue())
                .createdAt(mission.getCreatedAt())
                .updatedAt(mission.getUpdatedAt())
                .build();
    }
}