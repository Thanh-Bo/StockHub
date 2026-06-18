package com.stockhub.prices.dto;

import com.stockhub.company.dto.PricePoint;

import java.util.List;

public record PriceHistoryResponse(
    String ticker,
    String range,
    List<PricePoint> data
) {}
