package myexpressionfriend_api.statistics.expression.repository;

public interface ExpressionSessionTrendProjection {
    Integer getSessionNumber();
    Double getFinalAccuracy();
    Boolean getIsSuccess();
}
