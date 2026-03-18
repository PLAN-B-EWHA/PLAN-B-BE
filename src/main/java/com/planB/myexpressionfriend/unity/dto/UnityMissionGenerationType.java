package com.planB.myexpressionfriend.unity.dto;

/**
 * Unity 미션 생성 타입
 */
public enum UnityMissionGenerationType {

    EXPRESSION("Expression"),
    SITUATION("Situation");

    private final String missionTypeString;

    UnityMissionGenerationType(String missionTypeString) {
        this.missionTypeString = missionTypeString;
    }

    /**
     * Unity 미션 타입 문자열
     */
    public String getMissionTypeString() {
        return missionTypeString;
    }
}
