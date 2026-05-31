package myexpressionfriend_api.statistics.expression.repository;

public interface ExpressionAllEmotionTrendProjection {
    String getEmotionTarget();
    Integer getSessionNumber();
    Double getFinalAccuracy();
    Boolean getIsSuccess();
}
