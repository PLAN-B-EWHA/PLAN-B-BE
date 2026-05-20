package myexpressionfriend_api.statistics.dialogue.util;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScenarioWeekParser {

    private static final Pattern SCENARIO_WEEK_PATTERN = Pattern.compile("^W(\\d{2})_.*$");

    private ScenarioWeekParser() {
    }

    public static OptionalInt parseWeek(String scenarioId) {
        if (scenarioId == null || scenarioId.isBlank()) {
            return OptionalInt.empty();
        }
        Matcher matcher = SCENARIO_WEEK_PATTERN.matcher(scenarioId.trim());
        if (!matcher.matches()) {
            return OptionalInt.empty();
        }
        int week = Integer.parseInt(matcher.group(1));
        return (week >= 1 && week <= 16) ? OptionalInt.of(week) : OptionalInt.empty();
    }
}
