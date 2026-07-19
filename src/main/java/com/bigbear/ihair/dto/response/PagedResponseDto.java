package com.bigbear.ihair.dto.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PagedResponseDto<T> {
    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;

    public PagedResponseDto(Page<T> page) {
        content = page.getContent();
        this.page = page.getNumber();
        size = page.getSize();
        totalElements = page.getTotalElements();
        totalPages = page.getTotalPages();
        first = page.isFirst();
        last = page.isLast();
    }
}
