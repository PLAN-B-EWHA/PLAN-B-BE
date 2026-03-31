package myexpressionfriend_api.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Gemini LLM 연동 설정
 * <pre>
 * llm.gemini.enabled=true
 * llm.gemini.api-key=YOUR_KEY
 * llm.gemini.model=gemini-2.0-flash
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "llm.gemini")
@Getter
@Setter
public class GeminiProperties {

    private boolean enabled = false;
    private String apiKey;
    private String model = "gemini-2.0-flash";
    private String baseUrl = "https://generativelanguage.googleapis.com";
    private int timeoutMs = 45000;
}
