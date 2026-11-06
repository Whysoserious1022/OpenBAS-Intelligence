package com.openbas.intelligence.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FloorDto(
    UUID id,
    String externalId,
    UUID buildingId,
    String name,
    Integer floorNumber,
    String description,
    Double areaSqm,
    boolean active,
    Instant createdAt,
    List<ZoneDto> zones
) {}
