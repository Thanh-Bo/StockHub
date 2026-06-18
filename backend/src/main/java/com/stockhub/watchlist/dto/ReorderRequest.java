package com.stockhub.watchlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderRequest(
    @NotEmpty List<@NotBlank String> orderedTickers
) {}
