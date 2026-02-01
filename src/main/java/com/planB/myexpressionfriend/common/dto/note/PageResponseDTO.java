package com.planB.myexpressionfriend.common.dto.note;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 응답 공통 DTO
 */
@Getter
@Builder
public class PageResponseDTO<T> {

    private List<T> content; // 데이터 목록
    private int page; // 현재 페이지 (0부터 시작)
    private int size; // 페이지 크기
    private long totalElements; // 전체 데이터 개수
    private int totalPages; // 전체 페이지 수
    private boolean first; // 첫 페이지 여부
    private boolean last; // 마지막 페이지 여부
    private boolean hasNext; // 다음 페이지 존재 여부
    private boolean hasPrevious; // 이전 페이지 존재 여부

    /**
     * Spring Data의 Page를 PageResponseDTO로 변환
     */
    public static <T> PageResponseDTO<T> from(Page<T> page) {
        return PageResponseDTO.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * Page<Entity>를 Page<DTO>로 변환 후 PageResponseDTO로 변환
     */
    public static <T, E> PageResponseDTO<T> from(Page<E> page,
                                                 java.util.function.Function<E, T> converter) {
        Page<T> converted = page.map(converter);
        return from(converted);
    }
}