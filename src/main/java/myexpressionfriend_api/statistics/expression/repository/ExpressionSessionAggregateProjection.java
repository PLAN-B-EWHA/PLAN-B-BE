package myexpressionfriend_api.statistics.expression.repository;

public interface ExpressionSessionAggregateProjection {

    int getSessionCount();

    long getSuccessCount();

    double getAvgRetry();

    int getValidSessionCount();

    Double getAvgSessionDurationSec();
}
