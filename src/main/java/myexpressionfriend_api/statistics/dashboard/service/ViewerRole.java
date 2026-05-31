package myexpressionfriend_api.statistics.dashboard.service;

public enum ViewerRole {
    PARENT, THERAPIST;

    public boolean isTherapist() {
        return this == THERAPIST;
    }
}
