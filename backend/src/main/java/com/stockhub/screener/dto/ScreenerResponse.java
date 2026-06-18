package com.stockhub.screener.dto;

import java.util.List;

public record ScreenerResponse(
    List<ScreenerResultItem> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {}
