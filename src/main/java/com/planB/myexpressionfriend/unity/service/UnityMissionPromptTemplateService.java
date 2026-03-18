package com.planB.myexpressionfriend.unity.service;

import com.planB.myexpressionfriend.common.exception.InvalidRequestException;
import com.planB.myexpressionfriend.unity.dto.UnityMissionGenerationType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Unity 미션 프롬프트 템플릿 서비스
 */
@Service
@RequiredArgsConstructor
public class UnityMissionPromptTemplateService {

    private static final String EXPRESSION_TEMPLATE_PATH =
            "classpath:prompts/unity-expression-mission-prompt";
    private static final String SITUATION_TEMPLATE_PATH =
            "classpath:prompts/unity-situation-mission-prompt";

    private final ResourceLoader resourceLoader;

    /**
     * 타입별 프롬프트 템플릿 파일을 읽습니다.
     */
    public String loadTemplate(UnityMissionGenerationType generationType) {
        Resource resource = resourceLoader.getResource(resolveTemplatePath(generationType));

        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new InvalidRequestException("Unity 미션 프롬프트 템플릿을 읽을 수 없습니다.");
        }
    }

    private String resolveTemplatePath(UnityMissionGenerationType generationType) {
        if (generationType == UnityMissionGenerationType.SITUATION) {
            return SITUATION_TEMPLATE_PATH;
        }
        return EXPRESSION_TEMPLATE_PATH;
    }
}
