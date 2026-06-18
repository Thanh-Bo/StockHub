package com.stockhub.common.dto;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record ProblemDetail(
    String type,
    String title,
    int status,
    String detail,
    String instance,
    Map<String, List<String>> errors
) {}
