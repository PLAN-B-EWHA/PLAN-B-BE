package com.planB.myexpressionfriend.common.dto.mission;

import com.planB.myexpressionfriend.common.domain.mission.MissionStatus;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 미션 검색 조건 DTO
 */
@Getter
@Builder
public class MissionSearchDTO {

    private UUID childId;
    private MissionStatus status;
    private UUID therapistId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    @Builder.Default
    private String sortBy = "assignedAt";

    @Builder.Default
    private String sortDirection = "DESC";

    /**
     * 유효성 검증
     */
    public void validate() {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일은 종료일보다 이전이어야 합니다");
        }

        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("페이지 크기는 1-100 사이여야 합니다");
        }
    }

    /**
     * Pageable 객체 생성
     */
    public Pageable toPageable() {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}