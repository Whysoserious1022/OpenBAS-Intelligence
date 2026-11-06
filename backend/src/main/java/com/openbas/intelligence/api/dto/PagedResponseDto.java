package com.openbas.intelligence.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Wrapper for paginated API responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PagedResponseDto<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean last,
    boolean first
) {
    public static <T> PagedResponseDto<T> of(org.springframework.data.domain.Page<T> page) {
        return new PagedResponseDto<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast(),
            page.isFirst()
        );
    }
}
