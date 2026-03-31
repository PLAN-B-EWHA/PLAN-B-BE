package myexpressionfriend_api.unity.service;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.exception.InvalidRequestException;
import myexpressionfriend_api.unity.dto.UnityMissionGenerationType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Unity 미션 프롬프트 템플릿 로더
 * <p>
 * 템플릿 파일 위치:
 * <ul>
 *   <li>resources/prompts/unity-expression-mission-prompt</li>
 *   <li>resources/prompts/unity-situation-mission-prompt</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UnityMissionPromptTemplateService {

    private static final String EXPRESSION_TEMPLATE_PATH = "classpath:prompts/unity-expression-mission-prompt";
    private static final String SITUATION_TEMPLATE_PATH  = "classpath:prompts/unity-situation-mission-prompt";

    private final ResourceLoader resourceLoader;

    public String loadTemplate(UnityMissionGenerationType generationType) {
        Resource resource = resourceLoader.getResource(resolveTemplatePath(generationType));
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new InvalidRequestException("Unity 미션 프롬프트 템플릿을 읽을 수 없습니다: " + e.getMessage());
        }
    }

    private String resolveTemplatePath(UnityMissionGenerationType generationType) {
        return generationType == UnityMissionGenerationType.SITUATION
                ? SITUATION_TEMPLATE_PATH
                : EXPRESSION_TEMPLATE_PATH;
    }
}
