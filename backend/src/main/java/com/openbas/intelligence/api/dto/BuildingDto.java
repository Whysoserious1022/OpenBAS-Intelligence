package com.openbas.intelligence.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BuildingDto(
    UUID id,
    String externalId,
    String name,
    String description,
    String address,
    String city,
    String country,
    String timezone,
    Double totalAreaSqm,
    Integer floorCount,
    boolean active,
    boolean simulationEnabled,
    Instant createdAt,
    Instant updatedAt,
    List<FloorDto> floors
) {}
