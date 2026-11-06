package com.openbas.intelligence.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EquipmentDto(
    UUID id,
    String externalId,
    UUID zoneId,
    UUID buildingId,
    UUID floorId,
    String name,
    String equipmentType,
    String manufacturer,
    String model,
    String serialNumber,
    String operationalStatus,
    Double healthScore,
    Double minOperatingTempCelsius,
    Double maxOperatingTempCelsius,
    Double ratedPowerKw,
    Double ratedAirflowCfm,
    boolean simulationEnabled,
    boolean active,
    UUID parentEquipmentId,
    Instant createdAt,
    Instant updatedAt
) {}
