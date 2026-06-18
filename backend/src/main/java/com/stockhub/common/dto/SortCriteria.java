package com.stockhub.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SortCriteria(
    @NotBlank String field,
    @Pattern(regexp = "ASC|DESC") String direction
) {
    public SortCriteria {
        if (direction == null || direction.isBlank()) {
            direction = "DESC";
        }
    }

    public SortCriteria(String field) {
        this(field, "DESC");
    }
}
