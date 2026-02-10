package com.planB.myexpressionfriend.common.domain.mission;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 미션 상태
 */
@Getter
@RequiredArgsConstructor
public enum MissionStatus {

    ASSIGNED("할당됨", "치료사가 미션을 할당한 상태"),
    IN_PROGRESS("진행중", "부모가 미션을 시작한 상태"),
    COMPLETED("완료", "부모가 미션을 완료하고 사진을 업로드한 상태"),
    VERIFIED("검증완료", "치료사가 미션 완료를 확인한 상태"),
    CANCELLED("취소됨", "미션이 취소된 상태");

    private final String displayName;
    private final String description;

    /**
     * 문자열로 Enum 찾기 (대소문자 무시)
     */
    public static MissionStatus fromString(String value) {
        for (MissionStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("존재하지 않는 상태입니다: " + value);
    }

    /**
     * displayName으로 Enum 찾기
     */
    public static MissionStatus fromDisplayName(String displayName) {
        for (MissionStatus status : values()) {
            if (status.displayName.equals(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("존재하지 않는 상태입니다: " + displayName);
    }

    /**
     * 다음 상태로 전이 가능한지 확인
     */
    public boolean canTransitionTo(MissionStatus nextStatus) {
        return switch (this) {
            case ASSIGNED -> nextStatus == IN_PROGRESS || nextStatus == CANCELLED;
            case IN_PROGRESS -> nextStatus == COMPLETED || nextStatus == CANCELLED;
            case COMPLETED -> nextStatus == VERIFIED || nextStatus == CANCELLED;
            case VERIFIED -> false; // 검증 완료 후에는 상태 변경 불가
            case CANCELLED -> false; // 취소된 미션은 상태 변경 불가
        };
    }
}