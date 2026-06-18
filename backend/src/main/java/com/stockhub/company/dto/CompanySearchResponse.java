package com.stockhub.company.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CompanySearchResponse(
    UUID id,
    String ticker,
    String name,
    String sector,
    BigDecimal marketCap
) {}
