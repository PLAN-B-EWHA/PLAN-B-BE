package myexpressionfriend_api.scenario.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.scenario.dto.ScenarioBulkImportResultDTO;
import myexpressionfriend_api.scenario.dto.ScenarioDTO;
import myexpressionfriend_api.scenario.service.ScenarioService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * 앱 시작 시 classpath:data/scenarios/*.json 파일을 읽어 DB에 적재합니다.
 * 이미 존재하는 scenario_id는 건너뜁니다 (idempotent).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScenarioDataLoader {

    private final ScenarioService scenarioService;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void load() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;

        try {
            resources = resolver.getResources("classpath:data/scenarios/*.json");
        } catch (IOException e) {
            log.warn("[ScenarioDataLoader] 시나리오 파일 탐색 실패: {}", e.getMessage());
            return;
        }

        if (resources.length == 0) {
            log.info("[ScenarioDataLoader] data/scenarios/ 에 JSON 파일이 없습니다. 건너뜁니다.");
            return;
        }

        log.info("[ScenarioDataLoader] {}개 파일 발견 → 로딩 시작", resources.length);

        Arrays.stream(resources)
                .sorted((a, b) -> a.getFilename().compareTo(b.getFilename()))
                .forEach(this::loadFile);
    }

    private void loadFile(Resource resource) {
        String filename = resource.getFilename();
        try (InputStream is = resource.getInputStream()) {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(is);

            List<ScenarioDTO> scenarios = new java.util.ArrayList<>();

            if (root.isArray() && root.size() > 0 && root.get(0).isArray()) {
                // [[{...}, {...}], [{...}]] 형태 — 배열의 배열을 flat하게 병합
                for (com.fasterxml.jackson.databind.JsonNode batch : root) {
                    scenarios.addAll(objectMapper.convertValue(
                            batch, new TypeReference<>() {}));
                }
            } else {
                // [{...}, {...}] 형태 — 일반 배열
                scenarios = objectMapper.convertValue(root, new TypeReference<>() {});
            }

            ScenarioBulkImportResultDTO result = scenarioService.bulkImport(scenarios);

            log.info("[ScenarioDataLoader] {} → 저장: {}개, 건너뜀: {}개",
                    filename, result.savedCount(), result.skippedCount());

        } catch (IOException e) {
            log.error("[ScenarioDataLoader] {} 파싱 실패: {}", filename, e.getMessage());
        }
    }
}
