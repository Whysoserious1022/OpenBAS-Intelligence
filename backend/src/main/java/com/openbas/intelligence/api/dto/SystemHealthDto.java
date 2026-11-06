package com.openbas.intelligence.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SystemHealthDto(
    String status,
    Instant timestamp,
    Map<String, ComponentHealthDto> components
) {
    public record ComponentHealthDto(
        String status,
        String details,
        Long responseTimeMs
    ) {}
}
