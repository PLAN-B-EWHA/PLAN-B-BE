package com.planB.myexpressionfriend.common.service;

public interface ReportLlmClient {

    String generateReport(String prompt, int maxTokens, String modelName);
}
