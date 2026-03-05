package com.planB.myexpressionfriend.common.domain.mission;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 미션 카테고리
 * - EXPRESSION: 표정 인식
 * - EMOTION_RECOGNITION: 감정 인식
 * - COMMUNICATION: 소통
 */
@Getter
@RequiredArgsConstructor
public enum MissionCategory {

    EXPRESSION("표정 인식", "표정 짓기와 관련된 미션"),
    EMOTION_RECOGNITION("감정 인식", "타인의 표정을 보고 감정을 읽기"),
    COMMUNICATION("소통", "타인과 함께 소통하기");

    private final String displayName;
    private final String description;

    public static MissionCategory fromString(String value) {
        for (MissionCategory category : values()) {
            if (category.name().equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("존재하지 않는 카테고리입니다. " + value);
    }

    public static MissionCategory fromDisplayName(String displayName) {
        for (MissionCategory category : values()) {
            if (category.displayName.equals(displayName)) {
                return category;
            }
        }
        throw new IllegalArgumentException("존재하지 않는 카테고리입니다. " + displayName);
    }
}
