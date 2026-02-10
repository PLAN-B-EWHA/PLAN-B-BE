package com.planB.myexpressionfriend.common.domain.mission;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 미션 난이도
 * - BEGINNER: 처음 시작하는 아동
 * - INTERMEDIATE: 기본 개념을 이해한 아동
 * - ADVANCED: 심화 학습 단계*/
@Getter
@RequiredArgsConstructor
public enum MissionDifficulty {

    BEGINNER("초급", "처음 시작하는 아동"),
    INTERMEDIATE("중급", "기본 개념을 이해한 아동"),
    ADVANCED("고급", "심화 학습 단계");

    private final String displayName;
    private final String description;


    /**
     * 문자열로 Enum 찾기 (대소문자 무시)
     */
    public static MissionDifficulty fromString(String value) {
        for (MissionDifficulty category : values()) {
            if (category.displayName.equals(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("존재하지 않는 난이도 입니다: " + value);
    }

    /**
     * displayName으로 Enum 찾기
     */
    public static MissionDifficulty fromDisplayName(String displayName) {
        for (MissionDifficulty category : values()) {
            if (category.displayName.equals(displayName)) {
                return category;
            }
        }
        throw new IllegalArgumentException("존재하지 않는 난이도입니다: " + displayName);
    }


}
