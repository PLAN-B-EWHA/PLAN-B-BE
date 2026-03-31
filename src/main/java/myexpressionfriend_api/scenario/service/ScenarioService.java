package myexpressionfriend_api.scenario.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.scenario.domain.DialogueOption;
import myexpressionfriend_api.scenario.domain.Scenario;
import myexpressionfriend_api.scenario.domain.ScenarioDialogueTurn;
import myexpressionfriend_api.scenario.dto.DialogueTurnDTO;
import myexpressionfriend_api.scenario.dto.DialogueOptionDTO;
import myexpressionfriend_api.scenario.dto.ScenarioBulkImportResultDTO;
import myexpressionfriend_api.scenario.dto.ScenarioDTO;
import myexpressionfriend_api.scenario.repository.ScenarioRepository;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;

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
            toSave.add(toEntity(dto));
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

    // ── private: DTO → Entity ─────────────────────────────────────────

    private Scenario toEntity(ScenarioDTO dto) {
        ScenarioDTO.Metadata m = dto.metadata();
        ScenarioDTO.Cast c = dto.cast();

        Scenario scenario = Scenario.builder()
                .scenarioId(dto.scenarioId())
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
