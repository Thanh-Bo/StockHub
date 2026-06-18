package com.stockhub.watchlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddStockRequest(
    @NotBlank @Pattern(regexp = "^[A-Z]{1,5}$") String ticker
) {}
