package com.stockhub.company.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PricePoint(
    LocalDate date,
    BigDecimal close,
    BigDecimal adjustedClose,
    Long volume
) {}
