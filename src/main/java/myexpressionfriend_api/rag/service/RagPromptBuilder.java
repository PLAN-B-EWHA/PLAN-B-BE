package myexpressionfriend_api.rag.service;

import myexpressionfriend_api.rag.domain.RagUseCase;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class RagPromptBuilder {

    public static final String REPORT_DEFAULT_TEMPLATE = "report-generation-default";
    public static final String OFFLINE_MISSION_DEFAULT_TEMPLATE = "offline-mission-default";
    public static final String SCENARIO_DEFAULT_TEMPLATE = "scenario";

    private final ResourceLoader resourceLoader;

    public RagPromptBuilder(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String defaultTemplateKey(RagUseCase useCase) {
        return switch (useCase) {
            case GENERAL_REFERENCE -> throw new IllegalArgumentException("GENERAL_REFERENCE is only for indexing/search, not generation.");
            case REPORT_GENERATION -> REPORT_DEFAULT_TEMPLATE;
            case OFFLINE_MISSION_GENERATION -> OFFLINE_MISSION_DEFAULT_TEMPLATE;
            case SCENARIO_GENERATION -> SCENARIO_DEFAULT_TEMPLATE;
        };
    }

    public String normalizeTemplateKey(RagUseCase useCase, String requestedTemplateKey) {
        String templateKey = requestedTemplateKey == null || requestedTemplateKey.isBlank()
                ? defaultTemplateKey(useCase)
                : requestedTemplateKey.trim();

        if (!templateKey.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid RAG prompt template key.");
        }

        if (useCase == RagUseCase.REPORT_GENERATION && !templateKey.startsWith("report-generation")) {
            throw new IllegalArgumentException("Report generation must use a report-generation template.");
        }
        if (useCase == RagUseCase.OFFLINE_MISSION_GENERATION && !templateKey.startsWith("offline-mission")) {
            throw new IllegalArgumentException("Offline mission generation must use an offline-mission template.");
        }
        if (useCase == RagUseCase.SCENARIO_GENERATION && !templateKey.startsWith("scenario")) {
            throw new IllegalArgumentException("Scenario generation must use a scenario-generation template.");
        }

        return templateKey;
    }

    public String build(String templateKey, Map<String, String> variables) {
        String template = loadTemplate(templateKey);
        String prompt = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            prompt = prompt.replace("{{" + entry.getKey() + "}}", safe(entry.getValue()));
        }
        return prompt;
    }

    private String loadTemplate(String templateKey) {
        Resource resource = resourceLoader.getResource("classpath:prompts/rag/" + templateKey + ".txt");
        if (!resource.exists()) {
            throw new IllegalArgumentException("RAG prompt template not found: " + templateKey);
        }
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load RAG prompt template: " + templateKey, ex);
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
