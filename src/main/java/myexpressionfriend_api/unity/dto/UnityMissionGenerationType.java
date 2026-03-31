package myexpressionfriend_api.unity.dto;

public enum UnityMissionGenerationType {

    EXPRESSION("Expression"),
    SITUATION("Situation");

    private final String missionTypeString;

    UnityMissionGenerationType(String missionTypeString) {
        this.missionTypeString = missionTypeString;
    }

    public String getMissionTypeString() {
        return missionTypeString;
    }
}
