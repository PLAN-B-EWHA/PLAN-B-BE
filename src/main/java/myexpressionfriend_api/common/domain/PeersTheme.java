package myexpressionfriend_api.common.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum PeersTheme {

    INFORMATION_EXCHANGE    (1,  "정보 교환하기"),
    MAINTAINING_CONVERSATION(2,  "대화 유지하기"),
    FINDING_COMMON_GROUND   (3,  "공통점 찾기"),
    STARTING_CONVERSATION   (4,  "대화 시작하기"),
    EXITING_CONVERSATION    (5,  "대화 빠져나오기"),
    ELECTRONIC_COMMUNICATION(6,  "전자 통신"),
    CHOOSING_FRIENDS        (7,  "친구 선택하기"),
    USING_HUMOR             (8,  "유머 사용하기"),
    GOOD_SPORTSMANSHIP      (9,  "좋은 스포츠맨십"),
    PLAYING_TOGETHER        (10, "함께 놀기"),
    RESOLVING_CONFLICT      (11, "갈등 해결하기"),
    HANDLING_TEASING        (12, "놀림에 대처하기"),
    HANDLING_BULLYING       (13, "따돌림 대처"),
    HANDLING_CYBERBULLYING  (14, "사이버 불링 대처"),
    HANDLING_RUMORS         (15, "소문과 뒷담화 대처"),
    MANAGING_REPUTATION     (16, "평판 관리하기");

    private final int weekNumber;
    private final String displayName;

    PeersTheme(int weekNumber, String displayName) {
        this.weekNumber = weekNumber;
        this.displayName = displayName;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public static PeersTheme ofWeek(int weekNumber) {
        return Arrays.stream(values())
                .filter(t -> t.weekNumber == weekNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 주차: " + weekNumber));
    }

    @JsonCreator
    public static PeersTheme fromDisplayName(String displayName) {
        if (displayName == null) throw new IllegalArgumentException("테마 값이 없습니다.");
        // "정보 교환하기 (Trading Information)" → "정보 교환하기" 로 정규화
        String normalized = displayName.contains("(")
                ? displayName.substring(0, displayName.indexOf('(')).trim()
                : displayName.trim();
        try {
            return PeersTheme.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            // Fallback to legacy displayName matching below.
        }
        return Arrays.stream(values())
                .filter(t -> t.displayName.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 테마입니다: " + displayName));
    }
}
