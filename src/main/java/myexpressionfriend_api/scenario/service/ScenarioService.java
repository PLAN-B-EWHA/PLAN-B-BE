package myexpressionfriend_api.scenario.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.common.dto.common.PageResponseDTO;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import myexpressionfriend_api.game.domain.ScenarioSource;
import myexpressionfriend_api.game.repository.ChildScenarioProgressRepository;
import myexpressionfriend_api.player.service.GamePlayerSelectionService;
import myexpressionfriend_api.scenario.domain.ScenarioApprovalStatus;
import myexpressionfriend_api.scenario.domain.DialogueOption;
import myexpressionfriend_api.scenario.domain.Scenario;
import myexpressionfriend_api.scenario.domain.ScenarioDialogueTurn;
import myexpressionfriend_api.scenario.dto.AdminScenarioResponseDTO;
import myexpressionfriend_api.scenario.dto.DialogueTurnDTO;
import myexpressionfriend_api.scenario.dto.DialogueOptionDTO;
import myexpressionfriend_api.scenario.dto.ScenarioBulkImportResultDTO;
import myexpressionfriend_api.scenario.dto.ScenarioDTO;
import myexpressionfriend_api.scenario.dto.ScenarioStatusResponseDTO;
import myexpressionfriend_api.scenario.repository.ScenarioRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioRenderAssetNormalizer scenarioRenderAssetNormalizer;
    private final ChildScenarioProgressRepository childScenarioProgressRepository;
    private final GamePlayerSelectionService gamePlayerSelectionService;

    // ── Import ────────────────────────────────────────────────────────

    /**
     * 시나리오 일괄 저장.
     * 이미 존재하는 scenario_id는 건너뜁니다 (idempotent).
     */
    @Transactional
    @CacheEvict(value = "weeklyScenarios", allEntries = true)
    public ScenarioBulkImportResultDTO bulkImport(List<ScenarioDTO> dtos) {
        List<Scenario> toSave = new ArrayList<>();

        for (ScenarioDTO dto : dtos) {
            if (scenarioRepository.existsByScenarioId(dto.scenarioId())) {
                log.debug("[Import] 건너뜀 (already exists): {}", dto.scenarioId());
                continue;
            }
            toSave.add(toEntity(scenarioRenderAssetNormalizer.normalize(dto)));
        }

        scenarioRepository.saveAll(toSave);

        int skipped = dtos.size() - toSave.size();
        log.info("[Import] 완료 — 요청: {}, 저장: {}, 건너뜀: {}", dtos.size(), toSave.size(), skipped);

        return new ScenarioBulkImportResultDTO(dtos.size(), toSave.size(), skipped);
    }

    // ── 조회 (캐시) ───────────────────────────────────────────────────

    /**
     * 주차별 시나리오 전체 조회.
     * Caffeine 캐시 적용 — 키: week (1~16).
     * 추후 spring.cache.type=redis 로 변경하면 Redis 로 자동 전환됩니다.
     */
    @Cacheable(value = "weeklyScenarios", key = "#week")
    public List<ScenarioDTO> getScenariosForWeek(int week) {
        log.debug("[CACHE MISS] DB 조회 — week={}", week);
        return scenarioRepository.findAllByWeekWithFullDetail(week)
                .stream()
                .map(ScenarioDTO::from)
                .toList();
    }

    /** 단건 조회 (turns + options 포함) */
    public ScenarioDTO getScenario(String scenarioId) {
        Scenario scenario = scenarioRepository.findWithFullDetail(scenarioId)
                .orElseThrow(() -> new EntityNotFoundException("시나리오를 찾을 수 없습니다. id=" + scenarioId));
        return ScenarioDTO.from(scenario);
    }

    public ScenarioDTO getScenario(String scenarioId, UUID userId) {
        Scenario scenario = scenarioRepository.findWithFullDetail(scenarioId)
                .orElseThrow(() -> new EntityNotFoundException("시나리오를 찾을 수 없습니다. id=" + scenarioId));

        boolean isCompleted = gamePlayerSelectionService.findSelectedPlayableChild(userId)
                .map(child -> childScenarioProgressRepository
                        .findByChild_ChildIdAndScenarioId(child.getChildId(), scenario.getScenarioId())
                        .isPresent())
                .orElseGet(() -> {
                    log.debug("Selected game child not found. userId={}, scenarioId={}", userId, scenarioId);
                    return false;
                });

        return ScenarioDTO.from(scenario, isCompleted);
    }

    public PageResponseDTO<AdminScenarioResponseDTO> searchAdminScenarios(
            ScenarioApprovalStatus status,
            ScenarioSource source,
            Integer week,
            String keyword,
            Pageable pageable
    ) {
        String normalizedKeyword = keyword == null || keyword.isBlank()
                ? null
                : "%" + keyword.trim().toLowerCase() + "%";
        return PageResponseDTO.from(
                scenarioRepository.searchAdminScenarios(status, source, week, normalizedKeyword, pageable),
                AdminScenarioResponseDTO::from
        );
    }

    public List<ScenarioDTO> getPublishedServerScenarios(Integer week) {
        return getPublishedServerScenarios(week, null);
    }

    public List<ScenarioDTO> getPublishedServerScenarios(Integer week, UUID userId) {
        List<ScenarioSource> serverSources = List.copyOf(EnumSet.of(
                ScenarioSource.SERVER_LLM,
                ScenarioSource.SERVER_MANUAL
        ));

        List<Scenario> scenarios = week == null
                ? scenarioRepository.findAllByStatusAndSourcesWithFullDetail(
                ScenarioApprovalStatus.PUBLISHED, serverSources)
                : scenarioRepository.findAllByWeekAndStatusAndSourcesWithFullDetail(
                week, ScenarioApprovalStatus.PUBLISHED, serverSources);

        if (userId != null) {
            return withCompletionStatus(userId, scenarios);
        }

        return scenarios.stream()
                .map(ScenarioDTO::from)
                .toList();
    }

    private List<ScenarioDTO> withCompletionStatus(UUID userId, List<Scenario> scenarios) {
        if (scenarios.isEmpty()) {
            return List.of();
        }

        Child child = gamePlayerSelectionService.getSelectedPlayableChild(userId);
        List<String> scenarioIds = scenarios.stream()
                .map(Scenario::getScenarioId)
                .toList();
        Set<String> completedScenarioIds = Set.copyOf(
                childScenarioProgressRepository.findCompletedScenarioIds(child.getChildId(), scenarioIds));

        return scenarios.stream()
                .map(scenario -> ScenarioDTO.from(
                        scenario,
                        completedScenarioIds.contains(scenario.getScenarioId())))
                .toList();
    }

    @Transactional
    @CacheEvict(value = "weeklyScenarios", allEntries = true)
    public ScenarioStatusResponseDTO publish(String scenarioId, UUID reviewerId, String reviewNote) {
        Scenario scenario = findScenarioForReview(scenarioId);
        scenario.publish(reviewerId, reviewNote);
        return ScenarioStatusResponseDTO.from(scenario);
    }

    @Transactional
    @CacheEvict(value = "weeklyScenarios", allEntries = true)
    public ScenarioStatusResponseDTO reject(String scenarioId, UUID reviewerId, String reviewNote) {
        Scenario scenario = findScenarioForReview(scenarioId);
        scenario.reject(reviewerId, reviewNote);
        return ScenarioStatusResponseDTO.from(scenario);
    }

    @Transactional
    @CacheEvict(value = "weeklyScenarios", allEntries = true)
    public ScenarioStatusResponseDTO archive(String scenarioId, UUID reviewerId, String reviewNote) {
        Scenario scenario = findScenarioForReview(scenarioId);
        scenario.archive(reviewerId, reviewNote);
        return ScenarioStatusResponseDTO.from(scenario);
    }

    @Transactional
    @CacheEvict(value = "weeklyScenarios", allEntries = true)
    public ScenarioStatusResponseDTO updateApprovalStatus(
            String scenarioId,
            ScenarioApprovalStatus status,
            UUID reviewerId,
            String reviewNote
    ) {
        Scenario scenario = findScenarioForReview(scenarioId);
        scenario.changeApprovalStatus(status, reviewerId, reviewNote);
        return ScenarioStatusResponseDTO.from(scenario);
    }

    public boolean isPublishedServerScenario(String scenarioId, ScenarioSource source) {
        return scenarioRepository.existsByScenarioIdAndSourceAndApprovalStatus(
                scenarioId,
                source,
                ScenarioApprovalStatus.PUBLISHED
        );
    }

    private Scenario findScenarioForReview(String scenarioId) {
        return scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new EntityNotFoundException("시나리오를 찾을 수 없습니다. id=" + scenarioId));
    }

    // ── private: DTO → Entity ─────────────────────────────────────────

    private Scenario toEntity(ScenarioDTO dto) {
        ScenarioDTO.Metadata m = dto.metadata();
        ScenarioDTO.Cast c = dto.cast();

        Scenario scenario = Scenario.builder()
                .scenarioId(dto.scenarioId())
                .source(dto.source() != null ? dto.source() : ScenarioSource.SERVER_MANUAL)
                .approvalStatus(dto.approvalStatus() != null ? dto.approvalStatus() : ScenarioApprovalStatus.PUBLISHED)
                .week(m != null ? m.week() : null)
                .theme(m != null ? m.theme() : null)
                .relationshipStage(m != null ? m.relationshipStage() : null)
                .scenarioSeed(m != null ? m.scenarioSeed() : null)
                .lobbyTitle(m != null ? m.lobbyTitle() : null)
                .backgroundImageId(m != null ? m.backgroundImageId() : null)
                .mainCharacter(c != null ? c.mainCharacter() : null)
                .mainCharPos(c != null ? c.mainCharPos() : null)
                .subCharacters(c != null ? c.subCharacters() : null)
                .subCharPos(c != null ? c.subCharPos() : null)
                .finalLearningPoint(dto.finalSummary() != null ? dto.finalSummary().totalLearningPoint() : null)
                .build();

        if (dto.dialogueFlow() != null) {
            for (int i = 0; i < dto.dialogueFlow().size(); i++) {
                DialogueTurnDTO tDto = dto.dialogueFlow().get(i);

                ScenarioDialogueTurn turn = ScenarioDialogueTurn.builder()
                        .scenario(scenario)
                        .turnOrder(tDto.turnId() != null ? tDto.turnId() : i + 1)
                        .internalMonologue(tDto.internalMonologue())
                        .npcUtterance(tDto.npcUtterance())
                        .npcAnimation(tDto.npcAnimation())
                        .npcExpression(tDto.npcExpression())
                        .build();

                if (tDto.options() != null) {
                    for (int j = 0; j < tDto.options().size(); j++) {
                        DialogueOptionDTO oDto = tDto.options().get(j);
                        DialogueOption option = DialogueOption.builder()
                                .turn(turn)
                                .optionOrder(j)
                                .score(oDto.score())
                                .text(oDto.text())
                                .peersLogic(oDto.peersLogic())
                                .feedback(oDto.feedback())
                                .npcReaction(oDto.npcReaction())
                                .reactionAnimation(oDto.reactionAnimation())
                                .reactionExpression(oDto.reactionExpression())
                                .build();
                        turn.getOptions().add(option);
                    }
                }

                scenario.getDialogueFlow().add(turn);
            }
        }

        return scenario;
    }
}
