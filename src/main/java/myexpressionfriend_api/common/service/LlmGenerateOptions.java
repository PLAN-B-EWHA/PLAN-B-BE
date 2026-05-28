package myexpressionfriend_api.common.service;

public record LlmGenerateOptions(
        String think
) {
    public static LlmGenerateOptions none() {
        return new LlmGenerateOptions(null);
    }
}
