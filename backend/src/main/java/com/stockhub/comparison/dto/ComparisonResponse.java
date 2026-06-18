package com.stockhub.comparison.dto;

import java.util.List;

public record ComparisonResponse(
    List<CompanyComparisonRow> companies,
    IndustryAveragesResponse industryAverages
) {}
