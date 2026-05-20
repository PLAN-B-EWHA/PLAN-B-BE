package myexpressionfriend_api.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "expression")
@Getter
@Setter
public class ExpressionBaselineProperties {

    private Map<String, Integer> baselineDurationMs = new HashMap<>();

    public int getBaseline(String emotion) {
        return baselineDurationMs.getOrDefault(emotion.toLowerCase(), 5000);
    }
}
