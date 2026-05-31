package myexpressionfriend_api.notification.domain;

/**
 * 알림 타입별 제목/본문 문자열을 한 곳에서 관리한다.
 * 수정·번역이 필요할 때 이 파일만 건드리면 된다.
 */
public final class NotificationMessages {

    private NotificationMessages() {}

    public record Message(String title, String body) {}

    public static Message reportGenerated(String childName) {
        return new Message(
                "새 리포트가 도착했어요",
                childName + " 아이의 리포트가 치료사에 의해 발행되었습니다."
        );
    }

    public static Message weeklySummary(String childName) {
        return weeklySummary(childName, null);
    }

    public static Message weeklySummary(String childName, String topHighlight) {
        String body = topHighlight != null && !topHighlight.isBlank()
                ? topHighlight + " 자세한 내용은 앱에서 확인하세요."
                : childName + " 아동의 이번 주 대화·표정 게임 활동 요약을 확인해 보세요.";
        return new Message(
                "이번 주 " + childName + " 아동의 성장 요약이 도착했습니다",
                body
        );
    }

    public static Message homeworkSubmitted(String childName, String strategyLabel) {
        return new Message(
                childName + " 아동의 숙제가 제출됐습니다",
                "'" + strategyLabel + "' 미션 보고서가 보호자에 의해 제출됐습니다. 검토해 주세요."
        );
    }

    public static Message homeworkReviewed(String childName, String strategyLabel) {
        return new Message(
                "숙제 검토가 완료됐습니다",
                childName + " 아동의 '" + strategyLabel + "' 미션이 치료사에게 검토됐습니다."
        );
    }

    public static Message homeworkExpired(String childName, String strategyLabel) {
        return new Message(
                "미제출 숙제가 만료됐습니다",
                childName + " 아동의 '" + strategyLabel + "' 미션이 기한이 지나 만료됐습니다."
        );
    }

    public static Message childInactive(String childName, int inactiveDays, boolean hasHistory) {
        String body = hasHistory
                ? childName + " 아동이 " + inactiveDays + "일 이상 접속하지 않았습니다. 확인해 보세요."
                : childName + " 아동은 아직 게임 기록이 없습니다. 확인해 보세요.";
        return new Message(
                childName + " 아동이 " + inactiveDays + "일째 미접속 중입니다",
                body
        );
    }
}
