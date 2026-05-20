package myexpressionfriend_api.statistics.dialogue.controller;

import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import myexpressionfriend_api.statistics.dialogue.service.DialogueStatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/llm/dialogue")
@RequiredArgsConstructor
public class DialogueStatisticsAdminController {

    private final DialogueStatisticsService dialogueStatisticsService;

    @GetMapping("/rebuild")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rebuild(
            @RequestParam UUID childId
    ) {
        int processed = dialogueStatisticsService.rebuildForChild(childId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "childId", childId,
                "processedSessions", processed
        )));
    }
}
