package myexpressionfriend_api.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "llm")
@Getter
@Setter
public class LlmProperties {
    private boolean enabled = false;
    private String baseUrl = "http://localhost:11434";
    private String apiKey;
    private String modelFlash = "gemini-3-flash-preview";
    private String modelPro = "gemini-3-pro-preview";
    private String think;
    private int maxRetries = 5;
    private int maxBackoffSeconds = 64;

    // 2-6 v2.3 운영 안전장치
    private int maxZeroScoreTurnsPerChild = 150;
    private int manualRefreshCooldownHours = 24;
}
