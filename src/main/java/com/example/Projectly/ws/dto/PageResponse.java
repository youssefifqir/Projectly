package com.example.Projectly.ws.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated response wrapper")
public record PageResponse<T>(

        @Schema(description = "Page content")
        List<T> content,

        @Schema(description = "Current page number (0-based)")
        int page,

        @Schema(description = "Page size")
        int size,

        @Schema(description = "Total number of elements across all pages")
        long totalElements,

        @Schema(description = "Total number of pages")
        int totalPages,

        @Schema(description = "Whether this is the last page")
        boolean last
) {
    public static <T> PageResponse<T> from(final org.springframework.data.domain.Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
