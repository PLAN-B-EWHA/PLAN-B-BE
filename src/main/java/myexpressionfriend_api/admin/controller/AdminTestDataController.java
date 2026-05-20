package myexpressionfriend_api.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import myexpressionfriend_api.admin.dto.AdminGameRecordSeedRequestDTO;
import myexpressionfriend_api.admin.dto.AdminGameRecordSeedResponseDTO;
import myexpressionfriend_api.admin.service.AdminGameRecordSeedService;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/test-data")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "관리자 - 테스트 데이터", description = "관리자 전용 테스트 데이터 API")
public class AdminTestDataController {

    private final AdminGameRecordSeedService adminGameRecordSeedService;

    @PostMapping("/game-records")
    @Operation(summary = "아동 게임 기록 테스트 데이터 생성", description = "대시보드와 통계 기능 테스트를 위해 대화/표정 게임 세션을 생성하고 통계 요약을 갱신합니다.")
    public ResponseEntity<ApiResponse<AdminGameRecordSeedResponseDTO>> seedGameRecords(
            @Valid @RequestBody AdminGameRecordSeedRequestDTO request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "테스트용 게임 기록이 생성되었습니다.",
                adminGameRecordSeedService.seed(request)));
    }
}
