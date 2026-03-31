package myexpressionfriend_api.common.service;

/**
 * Gemini LLM 클라이언트 인터페이스
 * - Unity 미션 생성, 리포트 생성 등 LLM 기능에 사용
 */
public interface GeminiLlmClient {

    /**
     * @param prompt     LLM에 전달할 프롬프트
     * @param maxTokens  최대 출력 토큰 수
     * @param modelName  사용할 모델명 ("default"이면 GeminiProperties.model 사용)
     * @return LLM 텍스트 응답
     */
    String generate(String prompt, int maxTokens, String modelName);
}
