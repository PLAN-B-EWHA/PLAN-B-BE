package myexpressionfriend_api.homework.domain;

public enum HomeworkStatus {
    PENDING,
    SUBMITTED,
    REVIEWED,
    CANCELED,
    /** 기한이 지날 때까지 제출되지 않은 숙제. 스케줄러가 자동 전환한다. */
    EXPIRED
}
