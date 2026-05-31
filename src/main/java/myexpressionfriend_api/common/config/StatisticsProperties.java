package myexpressionfriend_api.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;

@Component
@ConfigurationProperties(prefix = "statistics")
@Getter
@Setter
public class StatisticsProperties {

    private int recommendedPerWeek = 3;

    /** 주의 시작 요일. 기본값 MONDAY (한국 기준). */
    private DayOfWeek weekStartDay = DayOfWeek.MONDAY;
    private SessionDuration sessionDuration = new SessionDuration();
    private Mastery mastery = new Mastery();
    private GraphPhase graphPhase = new GraphPhase();

    @Getter
    @Setter
    public static class Mastery {
        private double emaThreshold = 0.65;
        private int minSessions = 3;
    }

    @Getter
    @Setter
    public static class GraphPhase {
        private int midWeeks = 9;
        private int completeWeeks = 16;
    }

    @Getter
    @Setter
    public static class SessionDuration {
        private int minSec = 30;
        private int expressionMaxSec = 300;
    }
}
