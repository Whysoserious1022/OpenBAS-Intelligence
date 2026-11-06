package com.openbas.intelligence.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ZoneDto(
    UUID id,
    String externalId,
    UUID floorId,
    UUID buildingId,
    String name,
    String zoneType,
    Double areaSqm,
    Integer designOccupancy,
    Double comfortTempMinCelsius,
    Double comfortTempMaxCelsius,
    Double comfortHumidityMinPct,
    Double comfortHumidityMaxPct,
    boolean active,
    Instant createdAt
) {}
