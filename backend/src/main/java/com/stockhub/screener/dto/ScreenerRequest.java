package com.stockhub.screener.dto;

import com.stockhub.common.dto.PaginationRequest;
import com.stockhub.common.dto.SortCriteria;
import jakarta.validation.Valid;

import java.util.List;

public record ScreenerRequest(
    List<@Valid FilterCriteria> filters,
    SortCriteria sort,
    PaginationRequest pagination
) {
    public ScreenerRequest {
        if (filters == null) {
            filters = List.of();
        }
        if (sort == null) {
            sort = new SortCriteria("market_cap", "DESC");
        }
        if (pagination == null) {
            pagination = new PaginationRequest();
        }
    }
}
