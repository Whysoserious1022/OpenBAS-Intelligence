package com.openbas.intelligence.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnomalyEventDto(
    UUID id,
    String equipmentId,
    String deviceId,
    String zoneId,
    String buildingId,
    String anomalyType,
    String severity,
    String pointId,
    Double observedValue,
    Double expectedMin,
    Double expectedMax,
    String detectionMethod,
    String evidence,
    String description,
    Instant detectedAt,
    boolean resolved,
    Instant resolvedAt,
    String resolutionNote,
    Instant createdAt
) {}
