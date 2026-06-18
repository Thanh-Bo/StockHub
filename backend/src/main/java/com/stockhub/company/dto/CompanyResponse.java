package com.stockhub.company.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CompanyResponse(
    UUID id,
    String ticker,
    String name,
    String description,
    String sector,
    String industry,
    String headquarters,
    BigDecimal marketCap,
    Long employees,
    Integer foundedYear,
    String website,
    String logoUrl,
    boolean isActive
) {}
