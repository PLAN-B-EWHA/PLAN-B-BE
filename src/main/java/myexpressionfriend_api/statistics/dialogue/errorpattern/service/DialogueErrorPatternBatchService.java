package myexpressionfriend_api.statistics.dialogue.errorpattern.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.common.config.LlmProperties;
import myexpressionfriend_api.common.domain.PeersTheme;
import myexpressionfriend_api.game.repository.DialogueTurnRepository;
import myexpressionfriend_api.statistics.dialogue.errorpattern.domain.DialogueErrorPatternSummary;
import myexpressionfriend_api.statistics.dialogue.errorpattern.domain.ErrorPatternType;
import myexpressionfriend_api.statistics.dialogue.errorpattern.repository.DialogueErrorPatternSummaryRepository;
import myexpressionfriend_api.statistics.dialogue.errorpattern.repository.ZeroScoreTurnProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DialogueErrorPatternBatchService {

    private final ChildRepository childRepository;
    private final DialogueTurnRepository dialogueTurnRepository;
    private final DialogueErrorPatternSummaryRepository summaryRepository;
    private final LlmErrorPatternClassifier classifier;
    private final LlmProperties llmProperties;
    private final MeterRegistry meterRegistry;

    @Transactional
    public void runBiweeklyBatch() {
        Timer.Sample sample = Timer.start(meterRegistry);
        List<Child> children = childRepository.findAll();
        log.info("Error pattern batch start. childCount={}", children.size());
        int successCount = 0;
        int failureCount = 0;
        for (Child child : children) {
            try {
                refreshForChild(child.getChildId(), false, null);
                successCount++;
            } catch (Exception ex) {
                failureCount++;
                log.warn("Error pattern batch skipped childId={}, reason={}", child.getChildId(), ex.getMessage());
            }
        }
        meterRegistry.counter("statistics.error_pattern.batch.children", "result", "success").increment(successCount);
        meterRegistry.counter("statistics.error_pattern.batch.children", "result", "failure").increment(failureCount);
        sample.stop(meterRegistry.timer("statistics.error_pattern.batch.duration"));
        log.info("Error pattern batch finish.");
    }

    @Transactional
    public void refreshForChild(UUID childId, boolean manualRequest) {
        refreshForChild(childId, manualRequest, null, false);
    }

    @Transactional
    public void refreshForChild(UUID childId, boolean manualRequest, Integer maxTurnsOverride) {
        refreshForChild(childId, manualRequest, maxTurnsOverride, false);
    }

    @Transactional
    public void refreshForChild(UUID childId, boolean manualRequest, Integer maxTurnsOverride, boolean force) {
        Timer.Sample sample = Timer.start(meterRegistry);
        log.info("Error pattern refresh start. childId={}, manualRequest={}, maxTurnsOverride={}",
                childId, manualRequest, maxTurnsOverride);

        try {
            if (manualRequest && !force && isInCooldown(childId)) {
                meterRegistry.counter("statistics.error_pattern.refresh", "result", "cooldown").increment();
                log.info("Manual refresh skipped by cooldown. childId={}", childId);
                return;
            }

            List<ZeroScoreTurnProjection> turns = dialogueTurnRepository.findLatestZeroScoreTurns(childId);
            log.info("Zero-score turns loaded. childId={}, count={}", childId, turns.size());
            if (turns.isEmpty()) {
                meterRegistry.counter("statistics.error_pattern.refresh", "result", "empty").increment();
                log.info("No zero-score turns. childId={}", childId);
                return;
            }

            int max = (maxTurnsOverride != null && maxTurnsOverride > 0)
                    ? maxTurnsOverride
                    : llmProperties.getMaxZeroScoreTurnsPerChild();
            List<ZeroScoreTurnProjection> sampled = sampleTurns(turns, max);
            log.info("Zero-score turns sampled. childId={}, sampledCount={}, max={}", childId, sampled.size(), max);

            Map<PeersTheme, List<ZeroScoreTurnProjection>> byTheme =
                    sampled.stream().collect(Collectors.groupingBy(ZeroScoreTurnProjection::getTheme));
            log.info("Theme grouping complete. childId={}, themeCount={}", childId, byTheme.size());

            for (Map.Entry<PeersTheme, List<ZeroScoreTurnProjection>> entry : byTheme.entrySet()) {
                PeersTheme theme = entry.getKey();
                List<ZeroScoreTurnProjection> themeTurns = entry.getValue();
                log.info("Classify start. childId={}, theme={}, turnCount={}", childId, theme, themeTurns.size());

                List<LlmErrorPatternClassifier.TurnClassification> classified =
                        classifier.classifyInTriplicate(themeTurns, false);

                saveSummary(childId, theme, classified, llmProperties.getModelFlash());
                log.info("Classify saved. childId={}, theme={}, classifiedCount={}", childId, theme, classified.size());
            }

            meterRegistry.counter("statistics.error_pattern.refresh", "result", "success").increment();
            log.info("Error pattern refresh finish. childId={}", childId);
        } catch (RuntimeException ex) {
            meterRegistry.counter("statistics.error_pattern.refresh", "result", "failure").increment();
            throw ex;
        } finally {
            sample.stop(meterRegistry.timer("statistics.error_pattern.refresh.duration"));
        }
    }

    private boolean isInCooldown(UUID childId) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(llmProperties.getManualRefreshCooldownHours());
        return summaryRepository.existsRecentRefreshByChild(childId, threshold);
    }

    private List<ZeroScoreTurnProjection> sampleTurns(List<ZeroScoreTurnProjection> turns, int max) {
        if (turns.size() <= max) return turns;
        List<ZeroScoreTurnProjection> latest = turns.stream()
                .sorted(Comparator.comparing(ZeroScoreTurnProjection::getStartedAt).reversed())
                .limit(max)
                .toList();
        return new ArrayList<>(latest);
    }

    private void saveSummary(
            UUID childId,
            PeersTheme theme,
            List<LlmErrorPatternClassifier.TurnClassification> classified,
            String modelName
    ) {
        Child child = childRepository.findById(childId).orElseThrow();

        int total = classified.size();
        long consistentCount = classified.stream().filter(LlmErrorPatternClassifier.TurnClassification::consistent).count();

        Map<ErrorPatternType, Long> counts = classified.stream()
                .collect(Collectors.groupingBy(
                        LlmErrorPatternClassifier.TurnClassification::finalType,
                        () -> new EnumMap<>(ErrorPatternType.class),
                        Collectors.counting()
                ));

        double lecturing = ratio(counts.getOrDefault(ErrorPatternType.LECTURING, 0L), total);
        double criticism = ratio(counts.getOrDefault(ErrorPatternType.CRITICISM, 0L), total);
        double topicIgnore = ratio(counts.getOrDefault(ErrorPatternType.TOPIC_IGNORE, 0L), total);
        double rejection = ratio(counts.getOrDefault(ErrorPatternType.REJECTION, 0L), total);
        double unclassified = ratio(counts.getOrDefault(ErrorPatternType.UNCLASSIFIED, 0L), total);
        double reliability = ratio(consistentCount, total);

        DialogueErrorPatternSummary summary = summaryRepository.findByChild_ChildIdAndTheme(childId, theme)
                .orElse(DialogueErrorPatternSummary.builder()
                        .child(child)
                        .theme(theme)
                        .build());

        summary.updateRates(
                total,
                lecturing,
                criticism,
                topicIgnore,
                rejection,
                unclassified,
                reliability,
                modelName,
                LocalDateTime.now()
        );
        summaryRepository.save(summary);
    }

    private double ratio(long count, long total) {
        return total == 0 ? 0.0 : (double) count / total;
    }
}
