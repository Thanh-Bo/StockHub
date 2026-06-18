package com.stockhub.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PaginationRequest(
    @Min(0) int page,
    @Min(1) @Max(100) int size
) {
    public PaginationRequest {
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 25;
        }
    }

    public PaginationRequest() {
        this(0, 25);
    }
}
