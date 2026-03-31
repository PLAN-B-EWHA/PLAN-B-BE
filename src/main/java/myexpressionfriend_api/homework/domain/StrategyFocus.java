package myexpressionfriend_api.homework.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * PEERS 16주 커리큘럼 전략 주제
 *
 * <p>각 주차는 하나의 핵심 전략에 대응합니다.
 * homework_assignments.strategy_focus, homework_reports.strategy_applied,
 * homework_strategy_log.strategy 에서 공통으로 사용합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum StrategyFocus {

    INFORMATION_EXCHANGE    (1,  "정보 교환하기"),
    CONVERSATION_MAINTENANCE(2,  "대화 유지하기"),
    FINDING_COMMON_GROUND   (3,  "공통점 찾기"),
    CONVERSATION_INITIATION (4,  "대화 시작하기"),
    CONVERSATION_EXIT       (5,  "대화 빠져나오기"),
    DIGITAL_COMMUNICATION   (6,  "전자 통신"),
    FRIEND_SELECTION        (7,  "친구 선택하기"),
    HUMOR_USE               (8,  "유머 사용하기"),
    GOOD_SPORTSMANSHIP      (9,  "좋은 스포츠맨십"),
    PLAYING_TOGETHER        (10, "함께 놀기"),
    CONFLICT_RESOLUTION     (11, "갈등 해결하기"),
    HANDLING_TEASING        (12, "놀림에 대처하기"),
    HANDLING_EXCLUSION      (13, "따돌림 대처"),
    HANDLING_CYBERBULLYING  (14, "사이버 불링 대처"),
    HANDLING_RUMORS         (15, "소문과 뒷담화 대처"),
    REPUTATION_MANAGEMENT   (16, "평판 관리하기");

    private final int week;
    private final String description;

    /** week 번호로 전략 조회 */
    public static StrategyFocus ofWeek(int week) {
        for (StrategyFocus s : values()) {
            if (s.week == week) return s;
        }
        throw new IllegalArgumentException("유효하지 않은 주차입니다: " + week);
    }
}
