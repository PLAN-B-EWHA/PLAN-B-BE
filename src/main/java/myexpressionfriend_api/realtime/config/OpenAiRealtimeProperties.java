package myexpressionfriend_api.realtime.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "openai.realtime")
@Getter
@Setter
public class OpenAiRealtimeProperties {
    private boolean enabled = false;
    private String baseUrl = "https://api.openai.com";
    private String apiKey;
    private String model = "gpt-realtime";
    private String voice = "marin";
    private String instructions = "You are a friendly game companion for a child social-expression practice game.";
    private String modalities = "text";
}
