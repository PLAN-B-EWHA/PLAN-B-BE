package myexpressionfriend_api.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "statistics")
@Getter
@Setter
public class StatisticsProperties {

    private int recommendedPerWeek = 3;
    private SessionDuration sessionDuration = new SessionDuration();

    @Getter
    @Setter
    public static class SessionDuration {
        private int minSec = 30;
        private int expressionMaxSec = 300;
    }
}
