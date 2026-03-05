package com.planB.myexpressionfriend.common.dto.mission;

import com.planB.myexpressionfriend.common.domain.mission.AssignedMission;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class MissionReviewQueueItemDTO {

    private UUID missionId;
    private UUID childId;
    private String childName;
    private UUID therapistId;
    private String therapistName;
    private String templateTitle;
    private LocalDateTime dueDate;
    private LocalDateTime completedAt;
    private String parentNote;

    public static MissionReviewQueueItemDTO from(AssignedMission mission) {
        if (mission == null) {
            return null;
        }

        return MissionReviewQueueItemDTO.builder()
                .missionId(mission.getMissionId())
                .childId(mission.getChild() != null ? mission.getChild().getChildId() : null)
                .childName(mission.getChild() != null ? mission.getChild().getName() : null)
                .therapistId(mission.getTherapist() != null ? mission.getTherapist().getUserId() : null)
                .therapistName(mission.getTherapist() != null ? mission.getTherapist().getName() : null)
                .templateTitle(mission.getTemplate() != null ? mission.getTemplate().getTitle() : null)
                .dueDate(mission.getDueDate())
                .completedAt(mission.getCompletedAt())
                .parentNote(mission.getParentNote())
                .build();
    }
}

