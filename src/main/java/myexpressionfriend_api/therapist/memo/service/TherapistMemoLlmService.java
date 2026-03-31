package myexpressionfriend_api.therapist.memo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.child.domain.ExpressionTag;
import myexpressionfriend_api.child.dto.ChildDetailResponseDTO;
import myexpressionfriend_api.child.service.ChildQueryService;
import myexpressionfriend_api.common.service.GeminiLlmClient;
import myexpressionfriend_api.notification.domain.NotificationType;
import myexpressionfriend_api.notification.service.NotificationService;
import myexpressionfriend_api.therapist.memo.domain.TherapistMemo;
import myexpressionfriend_api.therapist.memo.repository.TherapistMemoRepository;
import myexpressionfriend_api.unity.domain.UnityGameResult;
import myexpressionfriend_api.unity.repository.UnityGameResultRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TherapistMemoLlmService {

    private final TherapistMemoRepository memoRepository;
    private final ChildQueryService childQueryService;
    private final UnityGameResultRepository gameResultRepository;
    private final GeminiLlmClient geminiLlmClient;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Value("classpath:prompts/therapist-memo-draft-prompt")
    private Resource promptTemplate;

    private static final int MAX_TOKENS = 2048;

    // ─── 명시적 재생성: 치료사가 직접 요청할 때만 호출 ─────────────────────
    // 임시저장(createMemo)에서는 절대 호출하지 않음.

    @Async
    @Transactional
    public void regenerateDraft(UUID memoId, String therapistFeedback) {
        log.info("[LLM] 치료사 메모 초안 재생성 memoId={}", memoId);
        generateDraftInternal(memoId, therapistFeedback, true);
    }

    // ─── 내부 구현 ────────────────────────────────────────────────────────

    private void generateDraftInternal(UUID memoId, String therapistFeedback, boolean forceOverwrite) {
        TherapistMemo memo = memoRepository.findById(memoId).orElse(null);
        if (memo == null) {
            log.warn("[LLM] 메모를 찾을 수 없음 memoId={}", memoId);
            return;
        }

        try {
            ChildDetailResponseDTO child = childQueryService.getChildDetail(
                    memo.getChild().getChildId(), memo.getTherapistId());

            List<UnityGameResult> recentResults = gameResultRepository
                    .findTop5ByGameSession_Child_ChildIdOrderByCreatedAtDesc(memo.getChild().getChildId());

            String prompt = buildPrompt(child, memo.getContent(), recentResults, therapistFeedback);
            String rawResponse = geminiLlmClient.generate(prompt, MAX_TOKENS, null);

            parseLlmResponse(rawResponse).ifPresentOrElse(
                    parsed -> {
                        String pc = parsed.path("parent_content").asText("");
                        String tip = parsed.path("home_practice_tip").asText("");
                        if (forceOverwrite) {
                            memo.forceApplyLlmDraft(pc, tip);
                        } else {
                            memo.applyLlmDraft(pc, tip);
                        }
                        log.info("[LLM] 치료사 메모 초안 저장 완료 memoId={}", memoId);

                        // 치료사에게 알림
                        notificationService.saveAndSend(
                                memo.getTherapistId(),
                                NotificationType.REPORT_GENERATED,
                                "메모 초안 준비 완료",
                                memo.getChild().getName() + " 아동 메모 초안이 준비됐습니다.",
                                memoId
                        );
                    },
                    () -> log.warn("[LLM] 응답 파싱 실패 memoId={}", memoId)
            );

        } catch (Exception e) {
            log.error("[LLM] 치료사 메모 초안 생성 실패 memoId={}: {}", memoId, e.getMessage(), e);
        }
    }

    private String buildPrompt(ChildDetailResponseDTO child, String therapistContent,
                               List<UnityGameResult> gameResults, String therapistFeedback) {
        String template = loadTemplate();

        String gameResultSummary = buildGameResultSummary(gameResults);
        String feedbackSection = therapistFeedback != null && !therapistFeedback.isBlank()
                ? "[치료사 피드백]\n" + therapistFeedback + "\n"
                : "";

        return template
                .replace("${childName}", safe(child.getName()))
                .replace("${childAge}", child.getAge() != null ? String.valueOf(child.getAge()) : "미상")
                .replace("${languageSkill}", child.getLanguageSkill() != null ? child.getLanguageSkill().name() : "정보 없음")
                .replace("${sensoryProcessing}", child.getSensoryProcessing() != null ? child.getSensoryProcessing().name() : "정보 없음")
                .replace("${preferredExpressions}", formatExpressions(child.getPreferredExpressions()))
                .replace("${difficultExpressions}", formatExpressions(child.getDifficultExpressions()))
                .replace("${interests}", safe(child.getInterests()))
                .replace("${specialNotes}", safe(child.getSpecialNotes()))
                .replace("${gameResultSummary}", gameResultSummary)
                .replace("${therapistContent}", safe(therapistContent))
                .replace("${therapistFeedback}", feedbackSection);
    }

    private String buildGameResultSummary(List<UnityGameResult> results) {
        if (results.isEmpty()) return "최근 게임 기록 없음";

        long successCount = results.stream().filter(r -> Boolean.TRUE.equals(r.getSuccess())).count();
        double avgScore = results.stream().mapToInt(UnityGameResult::getScore).average().orElse(0);
        return String.format("최근 %d회 중 성공 %d회, 평균 점수 %.0f점", results.size(), successCount, avgScore);
    }

    private java.util.Optional<JsonNode> parseLlmResponse(String raw) {
        try {
            // 1. 코드블록 제거
            String normalized = raw.trim()
                    .replaceAll("(?s)^```[a-zA-Z]*\\s*", "")
                    .replaceAll("```\\s*$", "")
                    .trim();

            // 2. JSON 범위 추출
            int start = normalized.indexOf('{');
            int end = normalized.lastIndexOf('}');
            if (start < 0 || end < 0 || start > end) {
                log.warn("[LLM] JSON 범위를 찾을 수 없음. preview={}", abbreviate(raw));
                return java.util.Optional.empty();
            }
            String jsonStr = normalized.substring(start, end + 1);

            // 3. JSON 문자열 값 내부의 literal 개행 제거 (파싱 실패 주원인)
            jsonStr = sanitizeJsonString(jsonStr);

            return java.util.Optional.of(objectMapper.readTree(jsonStr));
        } catch (Exception e) {
            log.warn("[LLM] JSON 파싱 오류: {} | preview={}", e.getMessage(), abbreviate(raw));
            return java.util.Optional.empty();
        }
    }

    /**
     * JSON 문자열 값 안에 들어온 literal 개행(\n, \r)을 공백으로 치환.
     * JSON spec 상 문자열 내 literal 개행은 허용되지 않으므로 Gemini 응답에서 자주 발생.
     */
    private String sanitizeJsonString(String json) {
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                sb.append(c);
                escape = false;
                continue;
            }
            if (c == '\\') {
                sb.append(c);
                escape = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                sb.append(c);
                continue;
            }
            if (inString && (c == '\n' || c == '\r')) {
                sb.append(' ');
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) return "(empty)";
        String s = value.replace("\r", " ").replace("\n", " ").trim();
        return s.length() <= 500 ? s : s.substring(0, 500) + "...";
    }

    private String loadTemplate() {
        try {
            return promptTemplate.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("LLM 프롬프트 템플릿 로드 실패", e);
        }
    }

    private String formatExpressions(Set<ExpressionTag> tags) {
        if (tags == null || tags.isEmpty()) return "없음";
        return tags.stream().map(ExpressionTag::name).collect(Collectors.joining(", "));
    }

    private String safe(String value) {
        return value != null ? value : "정보 없음";
    }
}
