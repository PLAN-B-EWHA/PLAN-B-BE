package com.planB.myexpressionfriend.common.dto.note;

import com.planB.myexpressionfriend.common.domain.note.NoteType;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 노트 검색/필터링 요청 DTO
 */
@Getter
@Builder
public class NoteSearchDTO {

    private UUID childId; // 필수
    private NoteType type; // 노트 타입 필터
    private UUID authorId; // 작성자 필터
    private String keyword; // 검색 키워드 (제목 + 본문)

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate; // 시작일시

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate; // 종료일시

    @Builder.Default
    private Integer page = 0; // 페이지 번호 (0부터 시작)

    @Builder.Default
    private Integer size = 20; // 페이지 크기

    @Builder.Default
    private String sortBy = "createdAt"; // 정렬 기준

    @Builder.Default
    private String sortDirection = "DESC"; // 정렬 방향

    /**
     * Pageable 객체 생성
     */
    public Pageable toPageable() {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    /**
     * 유효성 검증
     */
    public void validate() {
        if (childId == null) {
            throw new IllegalArgumentException("아동 ID는 필수입니다");
        }

        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("페이지 크기는 1-100 사이여야 합니다");
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일시는 종료일시보다 이전이어야 합니다");
        }
    }
}