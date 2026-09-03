package com.cowtrack.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * One page of a collection, with enough context for a client to page through it.
 *
 * <p>{@code totalItems} is the part that matters: without it a client cannot
 * know how many pages there are, which is why the cattle list used to derive its
 * page count from the length of the array it had just been handed and always
 * concluded there was exactly one page.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {
    private List<T> content;
    private int currentPage;
    private int totalPages;
    private long totalItems;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;

    /** Wraps a page of entities, mapping each one to its response form. */
    public static <E, T> PaginatedResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PaginatedResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.hasNext(),
                page.hasPrevious());
    }
}