package com.planB.myexpressionfriend.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 컨트롤러의 페이지/정렬 파라미터를 일관된 방식으로 Pageable로 변환한다.
 */
public final class PageableUtil {

    private PageableUtil() {
    }

    public static Pageable createPageable(
            int page,
            int size,
            String sortBy,
            String sortDirection,
            Sort.Direction defaultDirection
    ) {
        Sort.Direction direction = defaultDirection;
        if ("ASC".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.ASC;
        } else if ("DESC".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.DESC;
        }

        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    public static Pageable createPageable(int page, int size) {
        return PageRequest.of(page, size);
    }
}
