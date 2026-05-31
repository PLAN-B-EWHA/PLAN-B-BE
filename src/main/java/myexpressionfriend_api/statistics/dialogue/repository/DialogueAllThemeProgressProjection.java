package myexpressionfriend_api.statistics.dialogue.repository;

public interface DialogueAllThemeProgressProjection {
    String getTheme();
    Integer getWeekNumber();
    Double getScoreRate();
}
